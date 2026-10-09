package com.hippo.ehviewer.ui.settings

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.ehviewer.core.i18n.R
import com.ehviewer.core.ui.component.BlurredBar
import com.ehviewer.core.ui.component.SquircleShape
import com.ehviewer.core.ui.component.blurBackdropSource
import com.ehviewer.core.ui.component.rememberBlurBackdrop
import com.ehviewer.core.ui.icons.EhIcons
import com.ehviewer.core.ui.icons.filled.FormatListNumbered
import com.ehviewer.core.ui.icons.filled.Home
import com.ehviewer.core.ui.icons.filled.Subscriptions
import com.ehviewer.core.ui.icons.filled.Whatshot
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.ui.MainNavItem
import com.hippo.ehviewer.ui.MainNavConfig
import com.hippo.ehviewer.ui.MainNavItems
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.main.NavigationIcon
import com.hippo.ehviewer.ui.titleRes
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal fun MainNavItem.icon(): ImageVector =
    when (this) {
        MainNavItem.Home -> EhIcons.Default.Home
        MainNavItem.Subscription -> EhIcons.Default.Subscriptions
        MainNavItem.Whatshot -> EhIcons.Default.Whatshot
        MainNavItem.Toplist -> EhIcons.Default.FormatListNumbered
        MainNavItem.Favorites -> MiuixIcons.Favorites
        MainNavItem.History -> MiuixIcons.Recent
        MainNavItem.Downloads -> MiuixIcons.Download
        MainNavItem.Settings -> MiuixIcons.Settings
    }

// 底栏导航项自定义页：点击行切换显隐、拖拽 ≡ 排序，配置驱动移动端底栏渲染。
// Settings 项不允许隐藏：移动端没有其他设置入口，藏掉会连本页一起锁死。
@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.BottomNavItemsScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val raw by Settings.bottomNavItems.collectAsState()
    val config = remember(raw) { MainNavItems.decode(raw) }
    val rowHeight = 56.dp
    val rowHeightPx = with(LocalDensity.current) { rowHeight.toPx() }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    fun persist(next: MainNavConfig) {
        Settings.bottomNavItems.value = MainNavItems.encode(next)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
            ) {
                TopAppBar(
                    title = stringResource(id = R.string.desktop_nav_items_title),
                    navigationIcon = { NavigationIcon() },
                    scrollBehavior = scrollBehavior,
                    color = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface,
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .blurBackdropSource(backdrop)
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState()),
        ) {
            SmallTitle(
                text = stringResource(id = R.string.desktop_nav_items_title),
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
                        onToggle = {
                            if (item != MainNavItem.Settings) {
                                persist(MainNavItems.toggleHidden(config, item))
                            }
                        },
                        onDragStart = {
                            draggingIndex = index
                            dragOffset = 0f
                        },
                        onDrag = { dragOffset += it },
                        onDragEnd = {
                            val target = (index + (dragOffset / rowHeightPx).roundToInt())
                                .coerceIn(0, config.order.lastIndex)
                            persist(MainNavItems.move(config, item, target))
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
                text = stringResource(id = R.string.desktop_nav_items_hint),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
            )
            TextButton(
                text = stringResource(id = R.string.desktop_nav_items_reset),
                onClick = { persist(MainNavItems.defaultConfig()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}

// 单条导航项配置行：拖拽手柄（≡）+ 图标 + 标题 + 显隐标识（✓/○）。点击行切换显隐，手柄承载拖拽排序。
@Composable
private fun NavItemConfigRow(
    item: MainNavItem,
    visible: Boolean,
    dragging: Boolean,
    rowHeight: Dp,
    onToggle: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val background = when {
        dragging -> MiuixTheme.colorScheme.primary.copy(alpha = 0.16f)
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
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 拖拽手柄：仅此处消费拖拽手势，避免与整行点击切换显隐冲突
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(rowHeight)
                .pointerInput(item) {
                    detectDragGestures(
                        onDragStart = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDragStart()
                        },
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
        Icon(
            imageVector = item.icon(),
            contentDescription = null,
            tint = if (visible) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.width(24.dp),
        )
        Text(
            text = stringResource(id = item.titleRes),
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
