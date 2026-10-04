package com.ehviewer.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.scrollbar.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ehviewer.core.ui.component.SquircleShape
import com.ehviewer.core.ui.component.VerticalScrollbar
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 桌面模态对话框统一骨架：半透明遮罩点击关闭 + Miuix surface 超椭圆 Squircle 卡片 + 标题行与关闭钮。
// 内容区限高滚动：卡片整体不超过窗口，标题与关闭钮恒可见，调用方内容再多也无需自处理溢出。
@Composable
fun DesktopModalCard(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 440.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                    onDismiss()
                    true
                } else {
                    false
                }
            }
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = modifier
                .width(cardWidth)
                .clip(SquircleShape(16.dp))
                .background(MiuixTheme.colorScheme.surface)
                .clickable(enabled = false) {},
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .pointerHoverIcon(PointerIcon.Hand),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
            HorizontalDivider(color = MiuixTheme.colorScheme.outline.copy(alpha = 0.15f))
            // 内容区限高滚动并挂载 Miuix 配色滚动条（由根节点 LocalScrollbarStyle 提供样式），长内容滚动可见
            val contentScrollState = rememberScrollState()
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(contentScrollState)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content,
                )
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(contentScrollState),
                    isScrollInProgress = contentScrollState.isScrollInProgress,
                    modifier = Modifier.fillMaxHeight().padding(end = 2.dp),
                )
            }
        }
    }
}
