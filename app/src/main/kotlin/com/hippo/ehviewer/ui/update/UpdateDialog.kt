package com.hippo.ehviewer.ui.update

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ehviewer.core.i18n.R
import com.ehviewer.core.util.isAtLeastO
import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.ui.component.MarkdownText
import com.hippo.ehviewer.ui.openBrowser
import com.hippo.ehviewer.updater.Release
import com.hippo.ehviewer.util.FileUtils
import com.hippo.ehviewer.util.requestInstallPermission
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

private val DialogNotesMaxHeight = 240.dp
private val DialogNotesFade = 40.dp
private val DialogSectionSpacing = 10.dp

@Composable
fun UpdateDialog(
    show: Boolean,
    release: Release,
    onDismiss: () -> Unit,
    onIgnore: ((version: String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 切到新版本时清掉旧版本遗留的安装包与错误，避免出现"可安装"的假象。
    LaunchedEffect(release.version) {
        UpdateDownloadManager.discardStaleResult(release.version)
    }

    val plan = buildUpdateDialogPlan(
        currentVersion = BuildConfig.RAW_VERSION_NAME,
        release = release,
        download = UpdateDownloadSnapshot(
            releaseVersion = UpdateDownloadManager.releaseVersion,
            isDownloading = UpdateDownloadManager.isDownloading,
            progress = UpdateDownloadManager.downloadProgress,
            downloadedBytes = UpdateDownloadManager.downloadedBytes,
            totalBytes = UpdateDownloadManager.totalBytes,
            speedBytesPerSec = UpdateDownloadManager.downloadSpeed,
            doneFile = UpdateDownloadManager.downloadedFile,
            error = UpdateDownloadManager.downloadError,
        ),
        canIgnore = onIgnore != null,
    )

    val summaryText = if (plan.summary.publishedAt.isNotBlank()) {
        stringResource(
            R.string.update_dialog_summary_dated,
            plan.summary.currentVersion,
            plan.summary.latestVersion,
            plan.summary.publishedAt,
        )
    } else {
        stringResource(
            R.string.update_dialog_summary,
            plan.summary.currentVersion,
            plan.summary.latestVersion,
        )
    }

    WindowDialog(
        show = show,
        title = stringResource(R.string.update_dialog_title),
        summary = summaryText,
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 轻量通道标识：正式 / CI / 沙盒仍是用户判断来源的保留信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChannelChip(
                    text = when (plan.channel) {
                        UpdateChannel.Ci -> stringResource(R.string.update_channel_ci)
                        UpdateChannel.Official -> stringResource(R.string.update_channel_official)
                    },
                )
                if (release.apkSize > 0L) {
                    Text(
                        text = FileUtils.humanReadableByteCount(release.apkSize),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(DialogSectionSpacing))

            NotesSection(
                plan = plan,
                release = release,
            )

            if (plan.showIgnore && onIgnore != null) {
                TextButton(
                    text = stringResource(R.string.update_dialog_btn_ignore),
                    onClick = { onIgnore(release.version) },
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (plan.showProgress) {
                DownloadProgress(plan = plan)
            }

            if (plan.showBrowserFallback && plan.errorText != null) {
                Spacer(modifier = Modifier.height(DialogSectionSpacing))
                Text(
                    text = plan.errorText,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.error,
                )
            }

            if (plan.primary == UpdatePrimaryAction.Install) {
                Spacer(modifier = Modifier.height(DialogSectionSpacing))
                Text(
                    text = stringResource(R.string.update_dialog_ready_install),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            DialogActions(
                plan = plan,
                onDismiss = onDismiss,
                onInstall = {
                    val file = UpdateDownloadManager.installableFile(release.version)
                    if (file != null) {
                        coroutineScope.launch { installUpdatePackage(context, file) }
                    } else {
                        Toast.makeText(context, R.string.update_package_missing, Toast.LENGTH_LONG).show()
                    }
                },
                onStartDownload = {
                    // 先确认安装权限再下载，避免"下载成功却装不上"。
                    coroutineScope.launch {
                        val permitted = if (isAtLeastO) {
                            runCatching { with(context) { requestInstallPermission() } }.getOrDefault(false)
                        } else {
                            true
                        }
                        if (permitted) {
                            UpdateDownloadManager.startDownload(release, context)
                        } else {
                            Toast.makeText(context, R.string.permission_denied, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onOpenBrowser = {
                    with(context) { openBrowser(updateBrowserFallbackUrl(release)) }
                },
            )
        }
    }
}

@Composable
private fun NotesSection(
    plan: UpdateDialogPlan,
    release: Release,
) {
    if (!plan.hasNotes) {
        Text(
            text = stringResource(R.string.update_dialog_no_changelog),
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        return
    }

    var expanded by remember(release.version) { mutableStateOf(false) }
    val notes = remember(release.changelog, expanded) {
        updateDialogNotesState(release.changelog, expanded)
    }
    val scrollState = rememberScrollState()
    val hasContentAbove by remember { derivedStateOf { scrollState.value > 0 } }
    val hasContentBelow by remember { derivedStateOf { scrollState.maxValue > scrollState.value } }

    Text(
        text = stringResource(R.string.update_dialog_notes_title),
        style = MiuixTheme.textStyles.body1,
        fontWeight = FontWeight.SemiBold,
        color = MiuixTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(6.dp))

    // 唯一滚动容器：羽化层必须在外侧，否则量到的是内容总高，遮罩会落在滚出视口的位置。
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f))
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = DialogNotesMaxHeight)
                .verticalEdgeFade(
                    top = hasContentAbove,
                    bottom = hasContentBelow,
                    fade = DialogNotesFade,
                )
                .verticalScroll(scrollState),
        ) {
            MarkdownText(
                markdown = notes.visibleMarkdown,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (expanded || notes.hiddenLineCount > 0) {
        TextButton(
            text = stringResource(
                if (expanded) R.string.update_dialog_collapse_notes else R.string.update_dialog_expand_notes,
            ),
            onClick = { expanded = !expanded },
            colors = ButtonDefaults.textButtonColorsPrimary(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DownloadProgress(plan: UpdateDialogPlan) {
    val numbers = plan.progress ?: return
    val downloaded = FileUtils.humanReadableByteCount(numbers.downloadedBytes)
    val speedText = if (numbers.speedBytesPerSec > 0L) {
        "${FileUtils.humanReadableByteCount(numbers.speedBytesPerSec)}/s"
    } else {
        null
    }
    val detail = when {
        numbers.totalBytes > 0L -> {
            val total = FileUtils.humanReadableByteCount(numbers.totalBytes)
            "$downloaded / $total" + (speedText?.let { " · $it" }.orEmpty())
        }
        speedText != null -> stringResource(R.string.update_dialog_progress_unknown, downloaded, speedText)
        else -> stringResource(R.string.update_dialog_progress_unknown_no_speed, downloaded)
    }

    Spacer(modifier = Modifier.height(DialogSectionSpacing))
    LinearProgressIndicator(
        // 总长未知时传 null 走不确定进度，而不是钉死在 0%。
        progress = plan.progressFraction,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = detail,
        style = MiuixTheme.textStyles.footnote2,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    )
}

@Composable
private fun DialogActions(
    plan: UpdateDialogPlan,
    onDismiss: () -> Unit,
    onInstall: () -> Unit,
    onStartDownload: () -> Unit,
    onOpenBrowser: () -> Unit,
) {
    when (plan.primary) {
        UpdatePrimaryAction.Downloading -> {
            Button(
                onClick = {},
                enabled = false,
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.update_dialog_btn_downloading, plan.downloadPercent),
                    style = MiuixTheme.textStyles.button,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    text = stringResource(R.string.update_dialog_btn_cancel_download),
                    onClick = { UpdateDownloadManager.cancel() },
                    colors = ButtonDefaults.textButtonColors(textColor = MiuixTheme.colorScheme.error),
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = stringResource(R.string.update_dialog_btn_background_download),
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        UpdatePrimaryAction.Retry -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onStartDownload,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.update_dialog_btn_retry),
                        style = MiuixTheme.textStyles.button,
                    )
                }
                TextButton(
                    text = stringResource(R.string.update_dialog_btn_browser),
                    onClick = onOpenBrowser,
                    modifier = Modifier.weight(1f),
                )
            }
            TextButton(
                text = stringResource(R.string.update_dialog_btn_later),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        else -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = when (plan.primary) {
                        UpdatePrimaryAction.Install -> onInstall
                        UpdatePrimaryAction.OpenRelease -> onOpenBrowser
                        else -> onStartDownload
                    },
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = when (plan.primary) {
                            UpdatePrimaryAction.Install -> stringResource(R.string.update_dialog_btn_install)
                            UpdatePrimaryAction.OpenRelease -> stringResource(R.string.update_dialog_btn_open_release)
                            else -> stringResource(R.string.update_dialog_btn_update_now)
                        },
                        style = MiuixTheme.textStyles.button,
                    )
                }
                TextButton(
                    text = stringResource(R.string.update_dialog_btn_later),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ChannelChip(text: String) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.footnote2,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/**
 * 上下边缘羽化。只有在对应方向还有内容时才渐隐，滚到顶/底后恢复实心，避免"永远有雾"。
 */
private fun Modifier.verticalEdgeFade(
    top: Boolean,
    bottom: Boolean,
    fade: Dp,
): Modifier = graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val fadePx = fade.toPx()
        if (fadePx <= 0f || size.height <= fadePx * 2) return@drawWithContent
        val edge = (fadePx / size.height).coerceIn(0f, 0.5f)
        val stops = buildList {
            add(0f to if (top) Color.Transparent else Color.Black)
            if (top) add(edge to Color.Black)
            if (bottom) add((1f - edge) to Color.Black)
            add(1f to if (bottom) Color.Transparent else Color.Black)
        }
        drawRect(
            brush = Brush.verticalGradient(*stops.toTypedArray()),
            blendMode = BlendMode.DstIn,
        )
    }
