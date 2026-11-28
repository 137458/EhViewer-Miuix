package com.hippo.ehviewer.ui.main

import android.text.Html
import android.text.TextUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.parseAsHtml
import com.ehviewer.core.i18n.R
import com.ehviewer.core.model.GalleryComment
import com.hippo.ehviewer.ui.legacy.CoilImageGetter
import com.hippo.ehviewer.ui.legacy.LinkifyTextView
import com.hippo.ehviewer.util.ReadableTime
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GalleryCommentCard(
    modifier: Modifier = Modifier,
    comment: GalleryComment,
    onCardClick: () -> Unit,
    onUserClick: () -> Unit,
    onUrlClick: (String) -> Unit,
    maxLines: Int = Int.MAX_VALUE,
    ellipsis: Boolean = false,
    processComment: @Composable (GalleryComment, Html.ImageGetter) -> CharSequence = { c, ig -> c.comment.parseAsHtml(imageGetter = ig) },
) = with(comment) {
    val targetUrl = remember { mutableStateOf<String?>(null) }
    Card(
        onClick = {
            targetUrl.value?.also {
                onUrlClick(it)
                targetUrl.value = null
            } ?: onCardClick()
        },
        modifier = modifier,
    ) {
        val margin = dimensionResource(id = com.hippo.ehviewer.R.dimen.keyline_margin)
        Row(
            modifier = Modifier.padding(horizontal = margin, vertical = 8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val userText = if (uploader) stringResource(id = R.string.comment_user_uploader, user.orEmpty()) else user.orEmpty()
            Text(
                text = userText,
                modifier = Modifier.clickable(role = Role.Button, onClick = onUserClick),
                style = MiuixTheme.textStyles.body2,
                color = if (uploader) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = ReadableTime.getTimeAgo(time),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceSecondary,
            )
        }
        val textColor = MiuixTheme.colorScheme.onSurface.toArgb()
        val linkTextColor = MiuixTheme.colorScheme.primary.toArgb()
        // 图片加载完成只触发重绘：若把完成事件写成 Compose 状态，每加载一张图都会重组并
        // 重新解析一遍整段 HTML 注释
        val viewRef = remember { arrayOfNulls<LinkifyTextView>(1) }
        val imageGetter = remember(comment) { CoilImageGetter { viewRef[0]?.invalidate() } }
        val commentText = processComment(comment, imageGetter)
        AndroidView(
            factory = { context ->
                LinkifyTextView(context) { targetUrl.value = it }.apply {
                    setTextColor(textColor)
                    setLinkTextColor(linkTextColor)
                    this.maxLines = maxLines
                    if (ellipsis) ellipsize = TextUtils.TruncateAt.END
                }
            },
            modifier = Modifier.padding(start = margin, end = margin, bottom = 8.dp),
        ) { view ->
            // factory 只在首次创建时执行，主题/配色变化必须在这里同步到已存在的 View
            viewRef[0] = view
            view.setTextColor(textColor)
            view.setLinkTextColor(linkTextColor)
            view.text = commentText
        }
    }
}
