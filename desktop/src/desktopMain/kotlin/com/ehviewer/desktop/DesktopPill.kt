package com.ehviewer.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ehviewer.core.ui.component.SquircleShape
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 桌面端通用悬停胶囊操作钮：Squircle 裁切 + 常态衬底 + primary 半透明悬停叠加 + Hand 光标。
// 全库"文本胶囊微交互"（评论区操作栏、阅读器导航/跳转、设置字段、详情行动作、预览控制栏）共用同一交互 token，
// 文字配色/字重等站点差异由 content(hovered) 自行表达；禁用态不叠悬停色并回退默认光标。
@Composable
internal fun DesktopHoverPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = SquircleShape(6.dp),
    containerColor: Color = Color.Transparent,
    hoverOverlayColor: Color = MiuixTheme.colorScheme.primary.copy(alpha = 0.08f),
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
    content: @Composable BoxScope.(hovered: Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    Box(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .background(if (hovered && enabled) hoverOverlayColor else Color.Transparent)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        content(hovered)
    }
}

// 桌面端通知气泡：深底胶囊，点击任意处消失；可选动作标签悬停下划线，点击执行动作并随通知一并消失。
// Main 与 LibraryScreen 两处通知栈共用同一渲染，避免逻辑分叉。
@Composable
internal fun DesktopNotificationBubble(
    notice: DesktopNotification,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DesktopHoverPill(
        onClick = onDismiss,
        modifier = modifier,
        shape = SquircleShape(8.dp),
        containerColor = MiuixTheme.colorScheme.surfaceContainerHighest,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = notice.message,
                color = MiuixTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (notice.actionLabel != null) {
                val actionHover = remember { MutableInteractionSource() }
                val actionHovered by actionHover.collectIsHoveredAsState()
                Text(
                    text = notice.actionLabel,
                    color = MiuixTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (actionHovered) TextDecoration.Underline else null,
                    modifier = Modifier
                        .hoverable(actionHover)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable {
                            notice.onAction?.invoke()
                            onDismiss()
                        },
                )
            }
        }
    }
}
