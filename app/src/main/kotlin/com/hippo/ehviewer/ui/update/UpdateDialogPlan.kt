package com.hippo.ehviewer.ui.update

import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.updater.Release
import java.io.File

internal const val UPDATE_NOTES_PREVIEW_LINES = 12

internal enum class UpdateChannel { Official, Ci, Simulated }

internal enum class UpdatePrimaryAction { Download, OpenRelease, Install, Retry, Downloading }

internal enum class UpdateDismissAction { Later, Background }

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
    val dismiss: UpdateDismissAction,
    val showCancelDownload: Boolean,
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

internal fun updateDialogSummary(currentVersion: String, release: Release): UpdateDialogSummary =
    UpdateDialogSummary(
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
    val visible = lines.take(previewLines).joinToString("\n") + "\n…"
    return UpdateDialogNotesState(visible, lines.size - previewLines)
}

internal fun updateDownloadPercent(progress: Float): Int =
    (progress.coerceIn(0f, 1f) * 100).toInt()

internal fun updateDialogProgressFraction(snapshot: UpdateDownloadSnapshot): Float? {
    if (snapshot.totalBytes <= 0L) return null
    return (snapshot.downloadedBytes.toFloat() / snapshot.totalBytes).coerceIn(0f, 1f)
}

internal fun updateBrowserFallbackUrl(release: Release): String =
    release.downloadLink.takeIf { it.isNotBlank() }
        ?: release.releaseUrl.takeIf { it.isNotBlank() }
        ?: "https://github.com/${BuildConfig.REPO_NAME}/releases"

internal fun buildUpdateDialogPlan(
    currentVersion: String,
    release: Release,
    download: UpdateDownloadSnapshot,
    canIgnore: Boolean,
    isSimulated: Boolean,
): UpdateDialogPlan {
    val downloading = download.isDownloading
    val failed = !downloading && download.error != null
    val downloaded = !downloading && download.doneFile != null

    val primary = when {
        failed -> UpdatePrimaryAction.Retry
        downloaded -> UpdatePrimaryAction.Install
        downloading -> UpdatePrimaryAction.Downloading
        release.downloadLink.isBlank() -> UpdatePrimaryAction.OpenRelease
        else -> UpdatePrimaryAction.Download
    }

    return UpdateDialogPlan(
        summary = updateDialogSummary(currentVersion, release),
        channel = when {
            isSimulated -> UpdateChannel.Simulated
            release.isCI -> UpdateChannel.Ci
            else -> UpdateChannel.Official
        },
        primary = primary,
        downloadPercent = if (downloading) updateDownloadPercent(download.progress) else 0,
        dismiss = if (downloading) UpdateDismissAction.Background else UpdateDismissAction.Later,
        showCancelDownload = downloading,
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

    fun reset() {
        promptedVersion = null
    }
}
