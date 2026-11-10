package com.hippo.ehviewer.ui.update

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.updater.Release
import java.io.File

internal const val UPDATE_NOTES_PREVIEW_LINES = 12

internal enum class UpdateChannel { Official, Ci }

internal enum class UpdatePrimaryAction { Download, OpenRelease, Install, Retry, Downloading }

internal data class UpdateDialogSummary(
    val currentVersion: String,
    val latestVersion: String,
    val publishedAt: String,
)

internal data class UpdateProgressNumbers(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long,
)

internal data class UpdateDownloadSnapshot(
    val releaseVersion: String? = null,
    val isDownloading: Boolean = false,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val doneFile: File? = null,
    val error: String? = null,
)

internal data class UpdateDialogPlan(
    val summary: UpdateDialogSummary,
    val channel: UpdateChannel,
    val primary: UpdatePrimaryAction,
    val downloadPercent: Int,
    val showBrowserFallback: Boolean,
    val showIgnore: Boolean,
    val showProgress: Boolean,
    val progressFraction: Float?,
    val progress: UpdateProgressNumbers?,
    val errorText: String?,
    val hasNotes: Boolean,
)

internal data class UpdateDialogNotesState(
    val visibleMarkdown: String,
    val hiddenLineCount: Int,
)

internal fun updateDialogSummary(currentVersion: String, release: Release): UpdateDialogSummary = UpdateDialogSummary(
    currentVersion = currentVersion,
    latestVersion = release.version,
    publishedAt = release.publishedAt,
)

internal fun updateDialogNotesState(
    markdown: String,
    expanded: Boolean,
    previewLines: Int = UPDATE_NOTES_PREVIEW_LINES,
): UpdateDialogNotesState {
    val lines = markdown.trimEnd().lines()
    if (expanded || lines.size <= previewLines) {
        return UpdateDialogNotesState(markdown, 0)
    }
    var cut = previewLines
    // 截断点落在表格内部时回滚到表格块起点，避免预览里出现半张表格。
    if (isTableRow(lines[cut - 1]) && isTableRow(lines[cut])) {
        var start = cut - 1
        while (start > 0 && isTableRow(lines[start - 1])) start--
        if (start > 0) cut = start
    }
    val visible = lines.take(cut).joinToString("\n") + "\n…"
    return UpdateDialogNotesState(visible, lines.size - cut)
}

private fun isTableRow(line: String): Boolean = line.trimStart().startsWith("|")

internal fun updateDownloadPercent(progress: Float): Int = (progress.coerceIn(0f, 1f) * 100).toInt()

internal fun updateDialogProgressFraction(snapshot: UpdateDownloadSnapshot): Float? {
    if (snapshot.totalBytes <= 0L) return null
    return (snapshot.downloadedBytes.toFloat() / snapshot.totalBytes).coerceIn(0f, 1f)
}

internal fun updateBrowserFallbackUrl(release: Release): String {
    // CI 通道的下载链接是需要鉴权的 artifacts API，浏览器兜底应指向 Actions 运行页。
    if (release.isCI) {
        return release.releaseUrl.takeIf { it.isNotBlank() }
            ?: "https://github.com/${BuildConfig.REPO_NAME}/actions"
    }
    return release.downloadLink.takeIf { it.isNotBlank() }
        ?: release.releaseUrl.takeIf { it.isNotBlank() }
        ?: "https://github.com/${BuildConfig.REPO_NAME}/releases"
}

internal fun buildUpdateDialogPlan(
    currentVersion: String,
    release: Release,
    download: UpdateDownloadSnapshot,
    canIgnore: Boolean,
): UpdateDialogPlan {
    // 结果必须属于当前 release：切换版本后旧安装包/旧错误/旧进度一律视为无效，回落到可下载。
    val sameVersion = download.releaseVersion == release.version
    val downloading = download.isDownloading && sameVersion
    val failed = !downloading && sameVersion && download.error != null
    val downloaded = !downloading && sameVersion && download.doneFile != null

    val primary = when {
        failed -> UpdatePrimaryAction.Retry
        downloaded -> UpdatePrimaryAction.Install
        downloading -> UpdatePrimaryAction.Downloading
        release.downloadLink.isBlank() -> UpdatePrimaryAction.OpenRelease
        else -> UpdatePrimaryAction.Download
    }

    return UpdateDialogPlan(
        summary = updateDialogSummary(currentVersion, release),
        channel = if (release.isCI) UpdateChannel.Ci else UpdateChannel.Official,
        primary = primary,
        downloadPercent = if (downloading) updateDownloadPercent(download.progress) else 0,
        showBrowserFallback = failed,
        showIgnore = canIgnore && !downloading,
        showProgress = downloading,
        progressFraction = if (downloading) updateDialogProgressFraction(download) else null,
        progress = if (downloading) {
            UpdateProgressNumbers(
                downloadedBytes = download.downloadedBytes,
                totalBytes = download.totalBytes,
                speedBytesPerSec = download.speedBytesPerSec,
            )
        } else {
            null
        },
        errorText = if (failed) download.error else null,
        hasNotes = release.changelog.isNotBlank(),
    )
}

internal fun shouldPromptUpdate(
    version: String,
    ignoredVersion: String?,
    promptedVersion: String?,
    userInitiated: Boolean,
): Boolean {
    if (userInitiated) return true
    val target = version.trim()
    return target != ignoredVersion?.trim() && target != promptedVersion?.trim()
}

/**
 * 本次进程内已提示过的版本。只用于压制自动检查的重复弹窗；
 * 与忽略版本、提示间隔天数互不混用，也不持久化。
 */
internal object UpdatePromptTracker {
    @Volatile
    var promptedVersion: String? = null
        private set

    fun markPrompted(version: String) {
        promptedVersion = version
    }

    fun forget(version: String) {
        // 撤销忽略后允许同版本再次自动提示；只清掉这一版本的提示标记。
        if (promptedVersion == version.trim()) promptedVersion = null
    }
}

/**
 * 启动更新弹窗的进程级状态。remember 只活在组合里，旋转屏幕后弹窗会丢失；
 * 提到进程级后重建 Activity 仍能恢复同一版本的弹窗。
 */
internal object UpdateStartupPrompt {
    var release by mutableStateOf<Release?>(null)
}
