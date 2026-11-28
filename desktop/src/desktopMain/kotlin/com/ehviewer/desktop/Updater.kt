package com.ehviewer.desktop

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// 桌面壳当前版本：唯一来源是根 gradle.properties 的 ehviewer.versionName，
// 由 desktop/build.gradle.kts 的 generateBuildVersion 生成 BuildVersion.kt 后引用
const val DESKTOP_VERSION: String = BuildVersion.VERSION

private const val RELEASES_LATEST_URL = "https://api.github.com/repos/137458/EhViewer-Miuix/releases/latest"
const val RELEASES_PAGE_URL = "https://github.com/137458/EhViewer-Miuix/releases/latest"

data class UpdateInfo(val tag: String, val pageUrl: String)

// 版本位比较：忽略 v 前缀与 -SNAPSHOT 后缀，缺失位段补 0
internal fun segment(version: String, index: Int): Int = version.removePrefix("v").substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }.getOrElse(index) { 0 }

internal fun isNewer(remote: String, current: String): Boolean {
    for (i in 0..2) {
        val r = segment(remote, i)
        val c = segment(current, i)
        if (r != c) return r > c
    }
    return false
}

// 手动检查更新的三态结论：可更新（携带信息）/ 已是最新 / 不可判定（网络或解析失败，不得误报为已最新）
sealed interface UpdateCheckResult {
    data class Available(val info: UpdateInfo) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data object Unavailable : UpdateCheckResult
}

// 以注入的 fetch（默认真实网络查询）将原始结果映射为三态结论
suspend fun checkLatestReleaseStatus(
    currentVersion: String,
    fetch: suspend () -> UpdateInfo? = ::checkLatestRelease,
): UpdateCheckResult {
    val info = fetch() ?: return UpdateCheckResult.Unavailable
    return if (isNewer(info.tag, currentVersion)) UpdateCheckResult.Available(info) else UpdateCheckResult.UpToDate
}

// GitHub release JSON 的最小 DTO：只取 tag_name，其余字段（assets/body/author…）由 ignoreUnknownKeys 忽略
@Serializable
internal data class GitHubLatestRelease(
    @SerialName("tag_name") val tagName: String = "",
)

private val releaseJson = Json { ignoreUnknownKeys = true }

// 纯解析：畸形 JSON、缺 tag_name、空标签一律 null（调用方按不可判定处理，不误报“已最新”）
internal fun parseLatestReleaseTag(body: String): String? = try {
    releaseJson.decodeFromString(GitHubLatestRelease.serializer(), body).tagName.takeIf { it.isNotBlank() }
} catch (e: IllegalArgumentException) {
    // SerializationException 是 IllegalArgumentException 的子类，统一按“无法解析”处理
    null
}

// 查询最新 Release；网络不可达或响应异常返回 null（调用方按无更新处理）
suspend fun checkLatestRelease(): UpdateInfo? = withContext(Dispatchers.IO) {
    try {
        val response = acquireClient().get(RELEASES_LATEST_URL)
        if (!response.status.isSuccess()) return@withContext null
        val tag = parseLatestReleaseTag(response.bodyAsText()) ?: return@withContext null
        UpdateInfo(tag, RELEASES_PAGE_URL)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
