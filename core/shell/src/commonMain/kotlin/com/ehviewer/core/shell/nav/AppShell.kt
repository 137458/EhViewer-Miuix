package com.ehviewer.core.shell.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ehviewer.core.ui.component.FloatingBottomBar
import com.ehviewer.core.ui.component.FloatingBottomBarItem
import com.ehviewer.core.ui.component.blurBackdropSource
import com.ehviewer.core.ui.util.BottomBarInsetsCalculator
import com.ehviewer.core.ui.util.LocalBottomBarContentPadding
import com.ehviewer.core.ui.util.NavigationChrome
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop

// 单窗口自适应导航外壳（与 :app MainActivity 大屏形态同源）：
// Rail 形态左侧常驻全量项；BottomBar 形态为悬浮液态玻璃底栏，仅主级目的地可见。
// 壳层无状态：导航形态、选中项、主项集合全部由宿主计算注入。
@Composable
fun AppShell(
    items: List<NavItemSpec>,
    primaryItems: List<NavItemSpec>,
    selectedIndex: Int,
    primarySelectedIndex: Int,
    chrome: NavigationChrome,
    bottomBarVisible: Boolean,
    backdrop: LayerBackdrop?,
    onItemSelected: (index: Int) -> Unit,
    onPrimarySelected: (index: Int) -> Unit,
    railHeader: (@Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val navBarBottomPadding = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
    val bottomBarEdgePadding = BottomBarInsetsCalculator.calculateBarBottomPadding(navBarBottomPadding)
    val mainContentBottomPadding = BottomBarInsetsCalculator.calculateMainContentBottomPadding(
        isPrimaryDestination = bottomBarVisible,
        navigationChrome = chrome,
        navBarBottomPadding = navBarBottomPadding,
    )
    CompositionLocalProvider(LocalBottomBarContentPadding provides mainContentBottomPadding) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (chrome == NavigationChrome.Rail) {
                NavigationRail(
                    expanded = false,
                    modifier = Modifier
                        .fillMaxHeight()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Start + WindowInsetsSides.Top + WindowInsetsSides.Bottom,
                            ),
                        ),
                    defaultWindowInsetsPadding = false,
                    // 侧栏条目多于可用高度时可滚动，避免条目被裁切后无法点击
                    scrollState = rememberScrollState(),
                    header = railHeader ?: {},
                ) {
                    items.forEachIndexed { index, item ->
                        NavigationRailItem(
                            selected = index == selectedIndex,
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                onItemSelected(index)
                            },
                            icon = item.icon,
                            label = item.label,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blurBackdropSource(backdrop),
                ) {
                    content()
                }
                if (chrome == NavigationChrome.BottomBar) {
                    // Row 作用域内须 FQN：RowScope.AnimatedVisibility 扩展会抢解析（与桌面壳既有写法一致）
                    androidx.compose.animation.AnimatedVisibility(
                        visible = bottomBarVisible,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = bottomBarEdgePadding, start = 24.dp, end = 24.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(min = 320.dp, max = 540.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            FloatingBottomBar(
                                modifier = Modifier.fillMaxWidth(),
                                selectedIndex = { primarySelectedIndex.coerceAtLeast(0) },
                                onSelected = { index ->
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onPrimarySelected(index)
                                },
                                backdrop = backdrop,
                                tabsCount = primaryItems.size,
                            ) {
                                primaryItems.forEachIndexed { index, item ->
                                    FloatingBottomBarItem(
                                        onClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onPrimarySelected(index)
                                        },
                                        modifier = Modifier.semantics { selected = primarySelectedIndex == index },
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp),
                                        )
                                        Text(
                                            text = item.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (primarySelectedIndex == index) FontWeight.SemiBold else FontWeight.Normal,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 由完整导航集合按主项策略派生底栏集合；顺序即策略 primaryKeys 顺序
fun List<NavItemSpec>.toPrimaryItems(policy: NavPrimaryPolicy): List<NavItemSpec> = policy.primaryKeys.mapNotNull { key -> firstOrNull { it.key == key } }

// 宿主选中 key → 集合索引；未命中返回 -1，由调用方 clamp
fun List<NavItemSpec>.indexOfKey(key: String?): Int = indexOfFirst { it.key == key }
