package com.ehviewer.desktop

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.ehviewer.core.ui.component.SquircleShape
import com.hippo.ehviewer.client.parser.GalleryDetailParser
import dev.icerock.moko.resources.compose.stringResource
import java.nio.ByteBuffer
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 评论显示模型：上传者评论置顶（组内保持原相对顺序）；折叠态最多展示 3 条
private val URL_REGEX = Regex("""https?://[^\s<>"')\]]+""")

object DesktopCommentsModel {
    fun sortForDisplay(comments: List<GalleryComment>): List<GalleryComment> = comments.sortedBy { !it.uploader }

    fun visibleCount(comments: List<GalleryComment>, expanded: Boolean): Int = if (expanded) comments.size else minOf(3, comments.size)

    // 评论中的 http(s) 链接：句尾标点（.,;!?）不属于链接本体；去重保序
    // 评论时间：本地时区 yyyy-MM-dd HH:mm
    fun formatCommentTime(epochMillis: Long): String {
        val instant = java.time.Instant.ofEpochMilli(epochMillis)
        val local = instant.atZone(java.time.ZoneId.systemDefault())
        return java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(local)
    }

    fun extractUrls(text: String): List<String> = URL_REGEX
        .findAll(text)
        .map { it.value.trimEnd('.', ',', ';', '!', '?') }
        .distinct()
        .toList()
}

// 详情面板评论区：拉取画廊详情页经 Rust 解析（与 Android 侧同链路），折叠展示、右键复制评论文本。
// comments == null 表示加载中；加载失败可点击重试。
@Composable
fun DesktopCommentsSection(
    gallery: BaseGalleryInfo,
    comments: List<GalleryComment>?,
    loadFailed: Boolean,
    onRetry: () -> Unit,
    apiUid: Long = -1L,
    apiKey: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val loadingText = stringResource(MR.strings.desktop_reader_loading)
    val loadFailedText = stringResource(MR.strings.desktop_reader_load_failed)
    val retryText = stringResource(MR.strings.action_retry)
    val headerText = stringResource(MR.strings.gallery_comments)
    val noCommentsText = stringResource(MR.strings.no_comments)
    val moreText = stringResource(MR.strings.more_comment)
    val copyCommentText = stringResource(MR.strings.copy_comment_text)
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    // 投票：apiuid/apikey 就绪时可用；结果以状态行反馈（成功显分数，失败显原因）
    var votingCommentId by remember { mutableStateOf<Long?>(null) }
    var votedScore by remember { mutableStateOf<Int?>(null) }
    var voteError by remember { mutableStateOf<String?>(null) }
    val canVote = apiUid >= 0 && !apiKey.isNullOrEmpty()

    fun vote(comment: GalleryComment, vote: Int) {
        val key = apiKey ?: return
        if (votingCommentId != null) return
        votingCommentId = comment.id
        votedScore = null
        voteError = null
        coroutineScope.launch {
            val outcome = DesktopGalleryHydrator.voteComment(
                apiUid = apiUid,
                apiKey = key,
                gid = gallery.gid,
                token = gallery.token,
                commentId = comment.id,
                vote = vote,
            )
            votingCommentId = null
            outcome.fold(
                onSuccess = { r -> votedScore = r.score },
                onFailure = { voteError = it.message },
            )
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
        if (votingCommentId != null) {
            Text(
                text = loadingText,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
            )
        }
        votedScore?.let { score ->
            Text(
                text = stringResource(MR.strings.desktop_comment_vote_score, score),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
            )
        }
        voteError?.let { message ->
            Text(
                text = stringResource(MR.strings.desktop_comment_vote_error, message),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
            )
        }
        val loaded = comments
        when {
            loaded == null && loadFailed -> Row(
                modifier = Modifier.padding(top = 6.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = loadFailedText,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 12.sp,
                )
                Box(
                    modifier = Modifier
                        .clip(SquircleShape(6.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable(onClick = onRetry)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    Text(
                        text = retryText,
                        color = MiuixTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
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
                            CommentItem(
                                comment = item,
                                canVote = canVote,
                                isVoting = votingCommentId == item.id,
                                onVote = { vote(item, it) },
                            )
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
private fun CommentItem(
    comment: GalleryComment,
    canVote: Boolean = false,
    isVoting: Boolean = false,
    onVote: (Int) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SquircleShape(8.dp))
            .background(MiuixTheme.colorScheme.surfaceContainerHighest)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val anonymousText = stringResource(MR.strings.desktop_anonymous)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(
                text = comment.user.orEmpty().ifEmpty { anonymousText },
                color = MiuixTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (comment.uploader) {
                Box(
                    modifier = Modifier
                        .clip(SquircleShape(4.dp))
                        .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = stringResource(MR.strings.key_uploader),
                        color = MiuixTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Text(
                text = DesktopCommentsModel.formatCommentTime(comment.time),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 11.sp,
            )
        }
        if (canVote) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to "▲", -1 to "▼").forEach { (value, arrow) ->
                    // 已投状态取模型布尔（voteState 是投票人名单字符串，子串匹配会误报）
                    val active = if (value == 1) comment.voteUpEd else comment.voteDownEd
                    Box(
                        modifier = Modifier
                            .clip(SquircleShape(4.dp))
                            .background(
                                if (active) {
                                    MiuixTheme.colorScheme.primary.copy(alpha = 0.15f)
                                } else {
                                    Color.Transparent
                                },
                            )
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable(enabled = !isVoting) { onVote(value) }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = if (isVoting) "…" else arrow,
                            color = if (active) {
                                MiuixTheme.colorScheme.primary
                            } else {
                                MiuixTheme.colorScheme.onSurfaceVariantSummary
                            },
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
        Text(
            text = comment.comment,
            color = MiuixTheme.colorScheme.onSurface,
            fontSize = 12.sp,
        )
        // 评论内链接点击即经系统浏览器打开（链接本体仍随右键菜单可复制）
        remember(comment.comment) { DesktopCommentsModel.extractUrls(comment.comment) }.forEach { url ->
            Text(
                text = url,
                color = MiuixTheme.colorScheme.primary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable { DesktopBrowser.openUrl(url) },
            )
        }
    }
}
