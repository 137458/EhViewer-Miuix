package com.hippo.ehviewer.ui.update

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ehviewer.core.files.delete
import com.ehviewer.core.i18n.R
import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.download.downloadLocation
import com.hippo.ehviewer.ui.component.MarkdownText
import com.hippo.ehviewer.ui.openBrowser
import com.hippo.ehviewer.updater.AppUpdater
import com.hippo.ehviewer.updater.Release
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.FileUtils
import com.hippo.ehviewer.util.ReadableTime
import com.hippo.ehviewer.util.installPackage
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * Xiaomi HyperOS 3.0 / Miuix 旗舰级规范版本更新弹窗。
 * 具备版本跃迁横幅、三联元信息胶囊、带左侧重音与引用块的高级 Markdown 日志排版、
 * 流式实时速率计算、安装包拉起安装、浏览器降级下载，以及沙盒全链路动态模拟能力。
 */
@Composable
fun UpdateDialog(
    show: Boolean,
    release: Release,
    onDismiss: () -> Unit,
    onUpdate: (url: String) -> Unit = {},
    onIgnore: ((version: String) -> Unit)? = null,
    isSimulated: Boolean = false,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isDownloading = UpdateDownloadManager.isDownloading
    val downloadProgress = UpdateDownloadManager.downloadProgress
    val downloadedBytes = UpdateDownloadManager.downloadedBytes
    val totalBytes = UpdateDownloadManager.totalBytes
    val downloadSpeed = UpdateDownloadManager.downloadSpeed
    val downloadedFile = UpdateDownloadManager.downloadedFile
    val downloadError = UpdateDownloadManager.downloadError

    fun cancelDownload() {
        UpdateDownloadManager.cancel()
    }

    fun startDownload() {
        UpdateDownloadManager.startDownload(release, isSimulated, context)
    }

    WindowDialog(
        show = show,
        title = stringResource(R.string.update_dialog_title),
        onDismissRequest = {
            if (!isDownloading) {
                onDismiss()
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // ── 顶部：版本跃迁横幅 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // 当前版本
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "v${BuildConfig.RAW_VERSION_NAME}",
                        style = MiuixTheme.textStyles.body2.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                        ),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }

                // 跃迁动态箭头
                Text(
                    text = "➔",
                    style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Bold),
                    color = MiuixTheme.colorScheme.primary,
                )

                // 目标新版本高亮胶囊
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = release.version,
                        style = MiuixTheme.textStyles.body2.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        ),
                        color = MiuixTheme.colorScheme.primary,
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MiuixTheme.colorScheme.primary)
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.update_badge_new),
                            style = MiuixTheme.textStyles.body2.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                            ),
                            color = MiuixTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── 三联元信息胶囊栏 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 通道胶囊
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    val channelText = when {
                        isSimulated -> "Sandbox Preview"
                        release.isCI -> stringResource(R.string.update_channel_ci)
                        else -> stringResource(R.string.update_channel_official)
                    }
                    Text(
                        text = channelText,
                        style = MiuixTheme.textStyles.body2.copy(fontSize = 11.sp),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }

                // 体积胶囊
                if (release.apkSize > 0L) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = FileUtils.humanReadableByteCount(release.apkSize),
                            style = MiuixTheme.textStyles.body2.copy(fontSize = 11.sp),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 发布日期
                if (release.publishedAt.isNotBlank()) {
                    Text(
                        text = release.publishedAt,
                        style = MiuixTheme.textStyles.body2.copy(fontSize = 11.sp),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 更新日志展示视口 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp, max = 230.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f))
                    .border(0.5.dp, MiuixTheme.colorScheme.dividerLine.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (release.changelog.isNotBlank()) {
                    MarkdownText(
                        markdown = release.changelog,
                        modifier = Modifier.fillMaxWidth(),
                        baseFontSize = 13,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.update_dialog_no_changelog),
                        style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp, lineHeight = 18.sp),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }

            // ── 动态状态反馈区 ──
            if (isDownloading) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .padding(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val speedText = if (downloadSpeed > 0L) " · ${FileUtils.humanReadableByteCount(downloadSpeed)}/s" else ""
                        Text(
                            text = stringResource(R.string.update_dialog_downloading, speedText),
                            style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                            color = MiuixTheme.colorScheme.primary,
                        )
                        val percent = if (downloadProgress >= 0f) "${(downloadProgress * 100).toInt()}%" else ""
                        val sizeText = if (totalBytes > 0L) {
                            "${FileUtils.humanReadableByteCount(downloadedBytes)} / ${FileUtils.humanReadableByteCount(totalBytes)}"
                        } else {
                            FileUtils.humanReadableByteCount(downloadedBytes)
                        }
                        Text(
                            text = if (percent.isNotEmpty()) "$sizeText ($percent)" else sizeText,
                            style = MiuixTheme.textStyles.body2.copy(fontSize = 11.sp),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = downloadProgress,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else if (downloadedFile != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.10f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MiuixTheme.colorScheme.primary),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.update_dialog_ready_install),
                        style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        color = MiuixTheme.colorScheme.primary,
                    )
                }
            } else if (downloadError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MiuixTheme.colorScheme.error.copy(alpha = 0.10f))
                        .padding(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.update_dialog_download_failed, downloadError),
                        style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        color = MiuixTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.update_dialog_network_hint),
                        style = MiuixTheme.textStyles.body2.copy(fontSize = 11.sp),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 底部操作按钮栏 ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                when {
                    isDownloading -> {
                        Button(
                            onClick = { cancelDownload() },
                            colors = ButtonDefaults.buttonColors(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.update_dialog_btn_cancel_download))
                        }
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.update_dialog_btn_background_download))
                        }
                    }
                    downloadedFile != null -> {
                        Button(
                            onClick = { startDownload() },
                            colors = ButtonDefaults.buttonColors(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.update_dialog_btn_redownload))
                        }
                        Button(
                            onClick = {
                                if (isSimulated) {
                                    Toast.makeText(context, context.getString(R.string.update_msg_sandbox_done), Toast.LENGTH_SHORT).show()
                                } else {
                                    coroutineScope.launch {
                                        with(context) { installPackage(downloadedFile) }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            modifier = Modifier.weight(1.3f),
                        ) {
                            Text(stringResource(R.string.update_dialog_btn_install))
                        }
                    }
                    downloadError != null -> {
                        Button(
                            onClick = {
                                val url = release.releaseUrl.ifBlank { "https://github.com/${BuildConfig.REPO_NAME}/releases" }
                                with(context) { openBrowser(url) }
                            },
                            colors = ButtonDefaults.buttonColors(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.update_dialog_btn_browser))
                        }
                        Button(
                            onClick = { startDownload() },
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            modifier = Modifier.weight(1.2f),
                        ) {
                            Text(stringResource(R.string.update_dialog_btn_retry))
                        }
                    }
                    else -> {
                        if (onIgnore != null) {
                            Button(
                                onClick = { onIgnore(release.version) },
                                colors = ButtonDefaults.buttonColors(),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.update_dialog_btn_ignore))
                            }
                        }
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.update_dialog_btn_later))
                        }
                        Button(
                            onClick = { startDownload() },
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            modifier = Modifier.weight(1.3f),
                        ) {
                            Text(stringResource(R.string.update_dialog_btn_update_now))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── GitHub 发布详情外链 ──
            val releasePageUrl = release.releaseUrl.ifBlank { "https://github.com/${BuildConfig.REPO_NAME}/releases" }
            Text(
                text = stringResource(R.string.update_dialog_github_release_note),
                style = MiuixTheme.textStyles.body2.copy(fontSize = 11.sp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { with(context) { openBrowser(releasePageUrl) } }
                    .padding(vertical = 4.dp),
            )
        }
    }
}
