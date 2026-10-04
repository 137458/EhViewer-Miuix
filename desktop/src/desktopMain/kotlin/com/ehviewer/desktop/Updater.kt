package com.ehviewer.desktop

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 桌面壳当前版本（与 app versionName 对齐，发版时需同步）
const val DESKTOP_VERSION = "1.15.0"

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

// 查询最新 Release；网络不可达或响应异常返回 null（调用方按无更新处理）
suspend fun checkLatestRelease(): UpdateInfo? = withContext(Dispatchers.IO) {
    runCatching {
        val response = acquireClient().get(RELEASES_LATEST_URL)
        if (!response.status.isSuccess()) return@withContext null
        val tag = Regex("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").find(response.bodyAsText())
            ?.groupValues?.get(1) ?: return@withContext null
        UpdateInfo(tag, RELEASES_PAGE_URL)
    }.getOrNull()
}
