package com.ehviewer.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 阅读窗口骨架：页码状态 + 图片区 + 翻页控件。
// 图片获取链路（详情页→图片页→图片地址）由后续轮次接入，当前以占位呈现。
@Composable
fun ReaderScreen(
    gallery: com.ehviewer.core.model.BaseGalleryInfo,
    onClose: () -> Unit,
) {
    var page by remember { mutableIntStateOf(1) }
    val displayTitle = gallery.title?.takeIf { it.isNotBlank() }
        ?: gallery.titleJpn?.takeIf { it.isNotBlank() }
        ?: gallery.gid.toString()

    Column(modifier = Modifier.fillMaxSize().background(MiuixTheme.colorScheme.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = displayTitle,
                color = MiuixTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Page $page",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Text(
                text = "✕",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(onClick = onClose),
            )
        }
        HorizontalDivider()

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Image loading pipeline lands in a later iteration",
                color = Color.White.copy(alpha = 0.6f),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "◀ Prev",
                color = if (page > 1) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(enabled = page > 1) { page -= 1 }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Box(modifier = Modifier.weight(1f))
            Text(
                text = "Next ▶",
                color = MiuixTheme.colorScheme.primary,
                modifier = Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable { page += 1 }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}
