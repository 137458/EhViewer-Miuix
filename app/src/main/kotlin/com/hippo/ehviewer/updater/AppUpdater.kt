package com.hippo.ehviewer.updater

import com.ehviewer.core.files.write
import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.EhApplication.Companion.ktorClient
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.executeAndParseAs
import com.hippo.ehviewer.util.copyTo
import com.hippo.ehviewer.util.ensureSuccess
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.util.zip.ZipInputStream
import kotlin.time.Clock
import kotlinx.io.asSource
import moe.tarsin.coroutines.runSuspendCatching
import okio.FileSystem
import okio.Path
import tachiyomi.data.release.GithubArtifacts
import tachiyomi.data.release.GithubCommitComparison
import tachiyomi.data.release.GithubRelease
import tachiyomi.data.release.GithubRepo
import tachiyomi.data.release.GithubWorkflowRuns

private const val API_URL = "https://api.github.com/repos/${BuildConfig.REPO_NAME}"
private const val LATEST_RELEASE_URL = "$API_URL/releases/latest"

// 安装包只允许来自 GitHub 的 https 域名：更新链接可被服务端/MITM 改写，放开主机等于允许任意下载源
private val ALLOWED_UPDATE_HOSTS = listOf("github.com", "api.github.com", "githubusercontent.com")
private val ZIP_CONTENT_TYPES = setOf("application/zip", "application/x-zip-compressed", "multipart/x-zip")
private const val DOWNLOAD_TEMP_SUFFIX = ".tmp"

object AppUpdater {
    suspend fun checkForUpdate(
        forceCheck: Boolean = false,
        returnLatestIfNoUpdate: Boolean = false,
    ): Release? {
        val now = Clock.System.now()
        val interval = Settings.updateIntervalDays.value
        if (UpdateCheckGate.shouldCheck(forceCheck, Settings.lastUpdateTime, interval, now.epochSeconds)) {
            if (Settings.useCIUpdateChannel.value) {
                val curSha = BuildConfig.COMMIT_SHA
                val branch = runSuspendCatching {
                    ghStatement(API_URL).executeAndParseAs<GithubRepo>().defaultBranch
                }.getOrDefault("main")
                val workflowRunsUrl = "$API_URL/actions/workflows/ci.yml/runs?branch=$branch&event=push&status=success&per_page=1"
                val workflowRun = runSuspendCatching {
                    ghStatement(workflowRunsUrl).executeAndParseAs<GithubWorkflowRuns>().workflowRuns.firstOrNull()
                }.getOrNull()
                if (workflowRun != null) {
                    val shortSha = workflowRun.headSha.take(7)
                    val hasCIUpdate = shortSha != curSha
                    if (hasCIUpdate || returnLatestIfNoUpdate) {
                        val archiveUrl = runSuspendCatching {
                            ghStatement(workflowRun.artifactsUrl)
                                .executeAndParseAs<GithubArtifacts>()
                                .getDownloadLink()
                        }.getOrDefault("")
                        // A successful workflow may publish no artifact. Let the release
                        // channel fallback handle that case instead of returning a broken URL.
                        if (archiveUrl.isNotBlank()) {
                            // A successful check is throttled; failed checks remain retryable.
                            Settings.lastUpdateTime = now.epochSeconds
                            val changelog = runSuspendCatching {
                                val commitComparisonUrl = "$API_URL/compare/$curSha...$shortSha"
                                val result = ghStatement(commitComparisonUrl).executeAndParseAs<GithubCommitComparison>()
                                result.commits.joinToString("\n") { commit ->
                                    "- ${commit.commit.message.takeWhile { it != '\n' }} (@${commit.commit.author.name})"
                                }
                            }.getOrDefault(workflowRun.title)
                            return Release(
                                version = shortSha,
                                changelog = changelog,
                                downloadLink = archiveUrl,
                                releaseTitle = workflowRun.title,
                                releaseUrl = workflowRun.htmlUrl
                                    ?: "https://github.com/${BuildConfig.REPO_NAME}/actions/runs/${workflowRun.id}",
                                apkSize = 0L,
                                publishedAt = "",
                                isCI = true,
                                hasUpdate = hasCIUpdate,
                            )
                        }
                    }
                }
            }

            val curVersion = BuildConfig.RAW_VERSION_NAME
            // 不能吞掉异常：否则限流/网络失败会被当成「已是最新版本」，用户也看不到失败提示
            val release = ghStatement(LATEST_RELEASE_URL).executeAndParseAs<GithubRelease>()
            // Persist the throttle timestamp only after the response has been parsed.
            Settings.lastUpdateTime = now.epochSeconds
            return resolveRelease(release, curVersion, returnLatestIfNoUpdate)
        }
        return null
    }

    /**
     * 把远端 Release 与当前版本解析为待展示的 [Release]。
     *
     * [returnLatestIfNoUpdate] 为 true 时，即使已是最新版本也回传当前版本说明，供更新页常驻展示；
     * 为 false 时没有新版本就返回 null。
     */
    fun resolveRelease(
        release: GithubRelease,
        curVersion: String,
        returnLatestIfNoUpdate: Boolean,
    ): Release? {
        val latestVersion = release.version
        val hasUpdate = compareVersions(latestVersion, curVersion) > 0
        if (!hasUpdate && !returnLatestIfNoUpdate) return null
        val downloadLink = release.getDownloadLink()
        val matchedAsset = release.getMatchedAsset()
        // 有更新但缺少安装包时仍然返回，由弹窗引导用户去发布页，而不是把更新整个吞掉。
        return Release(
            version = latestVersion,
            changelog = release.info,
            downloadLink = downloadLink,
            releaseTitle = release.name ?: latestVersion,
            releaseUrl = release.releaseLink,
            apkSize = matchedAsset?.size ?: 0L,
            apkDigest = matchedAsset?.digest,
            publishedAt = release.publishedAt?.take(10).orEmpty(),
            isCI = false,
            hasUpdate = hasUpdate,
        )
    }

    fun compareVersions(v1: String, v2: String): Int {
        val clean1 = v1.trim().removePrefix("v").removePrefix("V")
        val clean2 = v2.trim().removePrefix("v").removePrefix("V")

        val base1 = clean1.substringBefore("-").substringBefore("_")
        val base2 = clean2.substringBefore("-").substringBefore("_")
        val pre1 = clean1.substring(base1.length).trimStart('-', '_')
        val pre2 = clean2.substring(base2.length).trimStart('-', '_')

        // 逐段比较：数字段按数值比，缺段当 0；非数字段（如 b_release / a_release 拆出的 "b"/"a"）按字符串比
        val segs1 = base1.split(".")
        val segs2 = base2.split(".")
        val maxLen = maxOf(segs1.size, segs2.size)

        for (i in 0 until maxLen) {
            val seg1 = segs1.getOrElse(i) { "" }
            val seg2 = segs2.getOrElse(i) { "" }
            val num1 = seg1.toIntOrNull()
            val num2 = seg2.toIntOrNull()
            val cmp = when {
                num1 != null && num2 != null -> num1.compareTo(num2)
                num1 != null -> if (seg2.isEmpty()) num1.compareTo(0) else 1
                num2 != null -> if (seg1.isEmpty()) 0.compareTo(num2) else -1
                else -> seg1.compareTo(seg2, ignoreCase = true)
            }
            if (cmp != 0) {
                return cmp
            }
        }

        // 主版本号相同时，正式版一律大于预发布版（1.16.0 > 1.16.0-rc1）
        if (pre1.isEmpty() && pre2.isNotEmpty()) return 1
        if (pre1.isNotEmpty() && pre2.isEmpty()) return -1
        return comparePrerelease(pre1, pre2)
    }

    // 预发布标识必须按标识逐个比较，且数字标识按数值比：直接比字符串会把 beta10 排在 beta2 前面
    private fun comparePrerelease(a: String, b: String): Int {
        val ids1 = a.split('-', '_', '.')
        val ids2 = b.split('-', '_', '.')
        for (i in 0 until maxOf(ids1.size, ids2.size)) {
            val id1 = ids1.getOrNull(i) ?: return -1
            val id2 = ids2.getOrNull(i) ?: return 1
            val num1 = id1.toIntOrNull()
            val num2 = id2.toIntOrNull()
            val cmp = when {
                num1 != null && num2 != null -> num1.compareTo(num2)
                num1 != null -> -1
                num2 != null -> 1
                else -> compareIdentifier(id1, id2)
            }
            if (cmp != 0) return cmp
        }
        return 0
    }

    // 标识内部也可能挂数字（beta2 / beta10），必须按"字母段-数字段"逐段比较，
    // 直接整串比字符串会把 beta10 排在 beta2 前面
    private fun compareIdentifier(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            if (a[i].isDigit() && b[j].isDigit()) {
                val startA = i
                while (i < a.length && a[i].isDigit()) i++
                val startB = j
                while (j < b.length && b[j].isDigit()) j++
                // 去掉前导零后先比位数再比字典序，避免数值溢出的同时也让 0010 == 10
                val digitsA = a.substring(startA, i).trimStart('0')
                val digitsB = b.substring(startB, j).trimStart('0')
                val cmp = digitsA.length.compareTo(digitsB.length).takeIf { it != 0 } ?: digitsA.compareTo(digitsB)
                if (cmp != 0) return cmp
            } else {
                val cmp = a[i].lowercaseChar().compareTo(b[j].lowercaseChar())
                if (cmp != 0) return cmp
                i++
                j++
            }
        }
        return when {
            i < a.length -> 1
            j < b.length -> -1
            else -> 0
        }
    }

    suspend fun downloadUpdate(
        url: String,
        path: Path,
        onProgress: ((progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit)? = null,
    ) {
        // 更新链接来自服务端响应，必须限定在 GitHub 的 https 域名内
        require(isAllowedUpdateUrl(url)) { "Refusing to download update from untrusted url: $url" }
        // 请求发出前只能按 URL 猜压缩包，用来挑 Accept；真正的判定在拿到响应头之后
        val isZipHint = url.substringBefore('?').endsWith(".zip", ignoreCase = true)
        val isGhApi = url.startsWith("https://api.github.com")
        val statement = if (isGhApi) {
            ghStatement(url) {
                if (!isZipHint) accept(ContentType.Application.OctetStream)
                timeout {
                    requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                    connectTimeoutMillis = 30_000
                    socketTimeoutMillis = 30_000
                }
                if (onProgress != null) {
                    onDownload { done, total ->
                        val progress = if (total != null && total > 0L) (done.toFloat() / total).coerceIn(0f, 1f) else 0f
                        onProgress(progress, done, total ?: 0L)
                    }
                }
            }
        } else {
            ktorClient.prepareGet(url) {
                timeout {
                    requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                    connectTimeoutMillis = 30_000
                    socketTimeoutMillis = 30_000
                }
                if (onProgress != null) {
                    onDownload { done, total ->
                        val progress = if (total != null && total > 0L) (done.toFloat() / total).coerceIn(0f, 1f) else 0f
                        onProgress(progress, done, total ?: 0L)
                    }
                }
            }
        }

        // 先写临时文件，成功后再原子替换：中途失败不能留下半截 apk 被当成可安装包
        val parent = path.parent ?: throw IllegalArgumentException("Update path has no parent: $path")
        val tmpPath = parent.resolve(path.name + DOWNLOAD_TEMP_SUFFIX)
        FileSystem.SYSTEM.delete(tmpPath, mustExist = false)
        try {
            statement.execute { response ->
                response.status.ensureSuccess()
                if (isZipResponse(response.headers[HttpHeaders.ContentType], url)) {
                    response.bodyAsChannel().toInputStream().use { stream ->
                        ZipInputStream(stream).use { zip ->
                            // 压缩包里有目录条目，也可能混入 mapping 等别的文件，只认第一个 apk
                            var entry = zip.nextEntry
                            while (entry != null && !isApkEntry(entry.name, entry.isDirectory)) {
                                entry = zip.nextEntry
                            }
                            checkNotNull(entry) { "No apk entry found in update archive" }
                            tmpPath.write { transferFrom(zip.asSource()) }
                        }
                    }
                } else {
                    response.bodyAsChannel().copyTo(tmpPath)
                }
            }
            FileSystem.SYSTEM.atomicMove(tmpPath, path)
        } catch (t: Throwable) {
            FileSystem.SYSTEM.delete(tmpPath, mustExist = false)
            throw t
        }
    }

    /**
     * 只接受 GitHub 域名的 https 更新链接，防止更新通道被改成任意下载源。
     */
    internal fun isAllowedUpdateUrl(url: String): Boolean {
        val parsed = runSuspendCatching { Url(url) }.getOrNull() ?: return false
        if (parsed.protocol != URLProtocol.HTTPS) return false
        val host = parsed.host.lowercase()
        return ALLOWED_UPDATE_HOSTS.any { host == it || host.endsWith(".$it") }
    }

    /**
     * 以响应的 content-type 为准判断压缩包：CI 产物的 URL 结尾是 /zip，但普通 release
     * 资源也可能直接指向 .zip，两者都不能只看 URL。
     */
    internal fun isZipResponse(contentType: String?, url: String): Boolean {
        val type = contentType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        if (type.isNotEmpty()) return type in ZIP_CONTENT_TYPES
        return url.substringBefore('?').lowercase().endsWith(".zip")
    }

    internal fun isApkEntry(name: String, isDirectory: Boolean): Boolean = !isDirectory && name.endsWith(".apk", ignoreCase = true)
}

private suspend inline fun ghStatement(
    url: String,
    builder: HttpRequestBuilder.() -> Unit = {},
) = ktorClient.prepareGet(url) { apply(builder) }

data class Release(
    val version: String,
    val changelog: String,
    val downloadLink: String,
    val releaseTitle: String = "",
    val releaseUrl: String = "",
    val apkSize: Long = 0L,
    val apkDigest: String? = null,
    val publishedAt: String = "",
    val isCI: Boolean = false,
    val hasUpdate: Boolean,
)
