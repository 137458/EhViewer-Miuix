package com.ehviewer.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.ehviewer.core.i18n.MR
import com.ehviewer.core.ui.component.SquircleShape
import com.ehviewer.core.ui.util.readableWidth
import dev.icerock.moko.resources.compose.stringResource
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 导航项自定义页（设计文档 §4.5 / §6.6）：勾选显示哪些导航项 + 拖拽 ≡ 排序。
// 配置以 DesktopNavItems 编码串持久化，主壳的侧栏/悬浮底栏按此列表渲染。
@Composable
fun NavItemsScreen(onBack: () -> Unit) {
    val raw by DesktopSettings.navItems.valueFlow().collectAsState(DesktopSettings.navItems.value)
    val config = remember(raw) { DesktopNavItems.decode(raw) }
    val rowHeight = 56.dp
    val rowHeightPx = with(LocalDensity.current) { rowHeight.toPx() }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    fun persist(next: DesktopNavConfig) {
        DesktopSettings.navItems.value = DesktopNavItems.encode(next)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .readableWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                ) {
                    Icon(
                        imageVector = MiuixIcons.Back,
                        contentDescription = stringResource(MR.strings.desktop_a11y_back),
                        tint = MiuixTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = stringResource(MR.strings.desktop_nav_items_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                )
                DesktopHoverPill(
                    onClick = { persist(DesktopNavItems.defaultConfig()) },
                    containerColor = MiuixTheme.colorScheme.surfaceContainerHighest,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = stringResource(MR.strings.desktop_nav_items_reset),
                        color = MiuixTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    SmallTitle(
                        text = stringResource(MR.strings.desktop_nav_items_title),
                        modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 4.dp),
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        config.order.forEachIndexed { index, item ->
                            val isDragging = draggingIndex == index
                            NavItemConfigRow(
                                item = item,
                                visible = item !in config.hidden,
                                dragging = isDragging,
                                rowHeight = rowHeight,
                                onToggle = { persist(DesktopNavItems.toggleHidden(config, item)) },
                                onDragStart = {
                                    draggingIndex = index
                                    dragOffset = 0f
                                },
                                onDrag = { dragOffset += it },
                                onDragEnd = {
                                    val target = (index + (dragOffset / rowHeightPx).roundToInt())
                                        .coerceIn(0, config.order.lastIndex)
                                    persist(DesktopNavItems.move(config, item, target))
                                    draggingIndex = null
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    draggingIndex = null
                                    dragOffset = 0f
                                },
                                modifier = Modifier
                                    .zIndex(if (isDragging) 1f else 0f)
                                    .graphicsLayer {
                                        translationY = if (isDragging) dragOffset else 0f
                                        scaleX = if (isDragging) 1.02f else 1f
                                        scaleY = if (isDragging) 1.02f else 1f
                                    },
                            )
                        }
                    }
                    Text(
                        text = stringResource(MR.strings.desktop_nav_items_hint),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

// 单条导航项配置行：拖拽手柄（≡）+ 标题 + 显隐标识（✓/○）。点击行切换显隐，手柄承载拖拽排序。
@Composable
private fun NavItemConfigRow(
    item: DesktopNavItem,
    visible: Boolean,
    dragging: Boolean,
    rowHeight: androidx.compose.ui.unit.Dp,
    onToggle: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val background = when {
        dragging -> MiuixTheme.colorScheme.primary.copy(alpha = 0.16f)
        hovered -> MiuixTheme.colorScheme.primary.copy(alpha = 0.06f)
        else -> MiuixTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0f)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight)
            .clip(SquircleShape(8.dp))
            .background(background)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggle,
            )
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 拖拽手柄：仅此处消费拖拽手势，避免与整行点击切换显隐冲突
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(rowHeight)
                .pointerHoverIcon(PointerIcon.Hand)
                .pointerInput(item) {
                    detectDragGestures(
                        onDragStart = { onDragStart() },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragCancel() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.y)
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "≡",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = stringResource(item.titleRes),
            color = if (visible) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .clip(SquircleShape(6.dp))
                .background(
                    if (visible) {
                        MiuixTheme.colorScheme.primary.copy(alpha = 0.15f)
                    } else {
                        MiuixTheme.colorScheme.surfaceContainerHighest
                    },
                )
                .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text(
                text = if (visible) "✓" else "○",
                color = if (visible) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
