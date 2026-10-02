package com.ehviewer.desktop

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ehviewer.core.i18n.MR
import com.ehviewer.core.model.BaseGalleryInfo
import com.ehviewer.core.model.GalleryComment
import com.ehviewer.core.util.LogPriority
import com.ehviewer.core.util.logcat
import com.hippo.ehviewer.client.parser.GalleryDetailParser
import dev.icerock.moko.resources.compose.stringResource
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 评论显示模型：上传者评论置顶（组内保持原相对顺序）；折叠态最多展示 3 条
object DesktopCommentsModel {
    fun sortForDisplay(comments: List<GalleryComment>): List<GalleryComment> = comments.sortedBy { !it.uploader }

    fun visibleCount(comments: List<GalleryComment>, expanded: Boolean): Int = if (expanded) comments.size else minOf(3, comments.size)
}

// 详情面板评论区：拉取画廊详情页经 Rust 解析（与 Android 侧同链路），折叠展示、右键复制评论文本。
// comments == null 表示加载中；加载失败可点击重试。
@Composable
fun DesktopCommentsSection(gallery: BaseGalleryInfo) {
    var comments by remember { mutableStateOf<List<GalleryComment>?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var loadFailed by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }
    val loadingText = stringResource(MR.strings.desktop_reader_loading)
    val loadFailedText = stringResource(MR.strings.desktop_reader_load_failed)
    val retryText = stringResource(MR.strings.action_retry)
    val headerText = stringResource(MR.strings.gallery_comments)
    val noCommentsText = stringResource(MR.strings.no_comments)
    val moreText = stringResource(MR.strings.more_comment)
    val copyCommentText = stringResource(MR.strings.copy_comment_text)
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(gallery.gid, reloadKey) {
        loadFailed = false
        runCatching {
            withContext(Dispatchers.IO) {
                val response = desktopGet(galleryWebUrl(gallery.gid, gallery.token))
                val buffer = response.toByteBuffer() ?: error("HTTP ${response.status}")
                GalleryDetailParser.parse(buffer).detail.comments.comments
            }
        }.onSuccess { list ->
            comments = list
        }.onFailure { e ->
            loadFailed = true
            logcat("Comments", LogPriority.WARN) { "COMMENTS_LOAD failed ${gallery.gid}: $e" }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(enabled = !comments.isNullOrEmpty()) { expanded = !expanded },
        ) {
            Text(
                text = if (comments.isNullOrEmpty()) headerText else "$headerText (${comments!!.size})",
                color = MiuixTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            if (!comments.isNullOrEmpty() && comments!!.size > 3) {
                Text(
                    text = if (expanded) "-" else "+",
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
        val loaded = comments
        when {
            loaded == null && loadFailed -> Text(
                text = "$loadFailedText ($retryText)",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
                modifier = Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable { reloadKey += 1 }
                    .padding(top = 4.dp),
            )
            loaded == null -> Text(
                text = loadingText,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
            )
            loaded.isEmpty() -> Text(
                text = noCommentsText,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
            )
            else -> {
                val display = remember(loaded, expanded) {
                    DesktopCommentsModel
                        .sortForDisplay(loaded)
                        .take(DesktopCommentsModel.visibleCount(loaded, expanded))
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    display.forEach { item ->
                        ContextMenuArea(
                            items = {
                                listOf(
                                    ContextMenuItem(copyCommentText) {
                                        clipboard.setText(AnnotatedString(item.comment))
                                    },
                                )
                            },
                        ) {
                            CommentItem(comment = item)
                        }
                    }
                    if (loaded.size > display.size) {
                        Text(
                            text = moreText,
                            color = MiuixTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable { expanded = true }
                                .padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItem(comment: GalleryComment) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MiuixTheme.colorScheme.surfaceVariant)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = comment.user.orEmpty().ifEmpty { "Anonymous" },
                color = MiuixTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (comment.uploader) {
                Text(
                    text = stringResource(MR.strings.key_uploader),
                    color = MiuixTheme.colorScheme.primary,
                    fontSize = 12.sp,
                )
            }
        }
        Text(
            text = comment.comment,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontSize = 12.sp,
        )
    }
}
