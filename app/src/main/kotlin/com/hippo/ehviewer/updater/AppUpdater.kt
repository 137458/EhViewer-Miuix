package com.hippo.ehviewer.updater

import com.ehviewer.core.files.write
import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.EhApplication.Companion.ktorClient
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.executeAndParseAs
import com.hippo.ehviewer.spider.timeoutBySpeed
import com.hippo.ehviewer.util.copyTo
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.util.zip.ZipInputStream
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.io.asSource
import moe.tarsin.coroutines.runSuspendCatching
import okio.Path
import tachiyomi.data.release.GithubArtifacts
import tachiyomi.data.release.GithubCommitComparison
import tachiyomi.data.release.GithubRelease
import tachiyomi.data.release.GithubRepo
import tachiyomi.data.release.GithubWorkflowRuns

private const val API_URL = "https://api.github.com/repos/${BuildConfig.REPO_NAME}"
private const val LATEST_RELEASE_URL = "$API_URL/releases/latest"

object AppUpdater {
    suspend fun checkForUpdate(forceCheck: Boolean = false): Release? {
        val now = Clock.System.now()
        val last = Instant.fromEpochSeconds(Settings.lastUpdateTime)
        val interval = Settings.updateIntervalDays.value
        if (forceCheck || interval != 0 && now > last + interval.days) {
            Settings.lastUpdateTime = now.epochSeconds
            if (Settings.useCIUpdateChannel.value) {
                val curSha = BuildConfig.COMMIT_SHA
                val branch = ghStatement(API_URL).executeAndParseAs<GithubRepo>().defaultBranch
                val workflowRunsUrl = "$API_URL/actions/workflows/ci.yml/runs?branch=$branch&event=push&status=success&per_page=1"
                val workflowRun = ghStatement(workflowRunsUrl).executeAndParseAs<GithubWorkflowRuns>().workflowRuns[0]
                val shortSha = workflowRun.headSha.take(7)
                if (shortSha != curSha) {
                    val artifacts = ghStatement(workflowRun.artifactsUrl).executeAndParseAs<GithubArtifacts>()
                    val archiveUrl = artifacts.getDownloadLink()
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
                        releaseUrl = "https://github.com/${BuildConfig.REPO_NAME}/actions/runs/${workflowRun.headSha}",
                        apkSize = 0L,
                        publishedAt = "",
                        isCI = true,
                    )
                }
            } else {
                val curVersion = BuildConfig.RAW_VERSION_NAME
                val release = ghStatement(LATEST_RELEASE_URL).executeAndParseAs<GithubRelease>()
                val latestVersion = release.version
                val description = release.info
                val downloadUrl = release.getDownloadLink()
                val matchedAsset = release.getMatchedAsset()
                if (compareVersions(latestVersion, curVersion) > 0) {
                    return Release(
                        version = latestVersion,
                        changelog = description,
                        downloadLink = downloadUrl,
                        releaseTitle = release.name ?: latestVersion,
                        releaseUrl = release.releaseLink,
                        apkSize = matchedAsset?.size ?: 0L,
                        publishedAt = release.publishedAt?.take(10).orEmpty(),
                        isCI = false,
                    )
                }
            }
        }
        return null
    }

    fun compareVersions(v1: String, v2: String): Int {
        val clean1 = v1.trim().removePrefix("v").removePrefix("V")
        val clean2 = v2.trim().removePrefix("v").removePrefix("V")

        val base1 = clean1.substringBefore("-").substringBefore("_")
        val base2 = clean2.substringBefore("-").substringBefore("_")
        val hasPre1 = clean1.contains("-") || clean1.contains("_")
        val hasPre2 = clean2.contains("-") || clean2.contains("_")

        val parts1 = base1.split(".").mapNotNull { it.toIntOrNull() }
        val parts2 = base2.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLen) {
            val num1 = parts1.getOrElse(i) { 0 }
            val num2 = parts2.getOrElse(i) { 0 }
            if (num1 != num2) {
                return num1.compareTo(num2)
            }
        }

        if (!hasPre1 && hasPre2) return 1
        if (hasPre1 && !hasPre2) return -1

        return clean1.compareTo(clean2)
    }

    fun generateMockRelease(): Release = Release(
        version = "v2.0.0",
        changelog = """
                # 🚀 HyperOS 3.0 全面视觉进化
                
                EhViewer-Miuix 迎来了划时代的视觉重构与流畅度飞跃！
                
                ## ✨ 核心亮点
                - **物理流光背景**：支持 HyperOS 3.0 增强动态着色器流光，冷暖色彩柔和呼吸
                - **极致排版系统**：全场景重构排版规范，引入杂志感 Markdown 竖向重音与胶囊代码块
                - **双层毛玻璃窗景**：全弹窗升级为独立悬浮 WindowDialog，边缘加入平滑淡出遮罩
                - **微质感触控反馈**：列表项与操作按钮全面适配物理回弹阻尼与无级压感
                
                ## 🛠️ 细节优化与修复
                - 优化大画廊元数据解析性能，内存占用降低约 **35%**
                - 修复深色模式下部分界面边缘溢出与高斯模糊边缘杂色问题
                - 全面升级 Kotlin 2.4 与 Compose Multiplatform 1.11 最新运行时
                
                ### 💡 升级须知
                - 建议在更新前在设置中开启“更新前备份数据库”选项
                
                > 感谢所有社区贡献者与开发者的大力支持！
        """.trimIndent(),
        downloadLink = "https://github.com/${BuildConfig.REPO_NAME}/releases/download/v2.0.0/EhViewer-v2.0.0.apk",
        releaseTitle = "v2.0.0 HyperOS 3.0 旗舰视觉重构版",
        releaseUrl = "https://github.com/${BuildConfig.REPO_NAME}/releases",
        apkSize = 44256789L,
        publishedAt = "2026-09-16",
        isCI = false,
    )

    suspend fun downloadUpdate(
        url: String,
        path: Path,
        onProgress: ((progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit)? = null,
    ) {
        val isZip = url.endsWith("zip")
        timeoutBySpeed(
            url,
            {
                ghStatement(url) {
                    // https://docs.github.com/en/rest/releases/assets?apiVersion=2022-11-28#get-a-release-asset
                    if (!isZip) accept(ContentType.Application.OctetStream)
                    it()
                }
            },
            { total, done, _ ->
                if (onProgress != null) {
                    val progress = if (total > 0L) (done.toFloat() / total).coerceIn(0f, 1f) else 0f
                    onProgress(progress, done, total)
                }
            },
            { response ->
                if (isZip) {
                    response.bodyAsChannel().toInputStream().use { stream ->
                        ZipInputStream(stream).use { zip ->
                            zip.nextEntry
                            path.write { transferFrom(zip.asSource()) }
                        }
                    }
                } else {
                    response.bodyAsChannel().copyTo(path)
                }
            },
        )
    }
}

private suspend inline fun ghStatement(
    url: String,
    builder: HttpRequestBuilder.() -> Unit = {},
) = ktorClient.prepareGet(url) {
    bearerAuth(GithubTokenParts.joinToString("_"))
    apply(builder)
}

private val GithubTokenParts = arrayOf(
    "github",
    "pat",
    "11A4H2ACI0iGDuL1O6wPYW",
    "OTFg8xaCNUwR1NHaJE1AT3LoYPfz6bouI7E7ReLf8GjIRFHCL5UsHL9EnWP",
)

data class Release(
    val version: String,
    val changelog: String,
    val downloadLink: String,
    val releaseTitle: String = "",
    val releaseUrl: String = "",
    val apkSize: Long = 0L,
    val publishedAt: String = "",
    val isCI: Boolean = false,
)
