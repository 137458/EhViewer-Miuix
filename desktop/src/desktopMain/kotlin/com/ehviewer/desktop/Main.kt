package com.ehviewer.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.scrollbar.LocalScrollbarStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ehviewer.core.database.model.HistoryInfo
import com.ehviewer.core.database.model.LocalFavoriteInfo
import com.ehviewer.core.i18n.MR
import com.ehviewer.core.model.BaseGalleryInfo
import com.ehviewer.core.shell.nav.AppShell
import com.ehviewer.core.shell.nav.NavItemSpec
import com.ehviewer.core.shell.nav.NavPrimaryPolicy
import com.ehviewer.core.shell.nav.indexOfKey
import com.ehviewer.core.shell.nav.toPrimaryItems
import com.ehviewer.core.ui.component.BlurredBar
import com.ehviewer.core.ui.component.LocalBackdrop
import com.ehviewer.core.ui.component.SquircleShape
import com.ehviewer.core.ui.component.blurBackdropSource
import com.ehviewer.core.ui.component.scrollbarStyle
import com.ehviewer.core.ui.icons.EhIcons
import com.ehviewer.core.ui.icons.filled.Bookmarks
import com.ehviewer.core.ui.icons.filled.FormatListNumbered
import com.ehviewer.core.ui.icons.filled.Home
import com.ehviewer.core.ui.icons.filled.Subscriptions
import com.ehviewer.core.ui.icons.filled.Whatshot
import com.ehviewer.core.ui.theme.rememberMiuixThemeController
import com.ehviewer.core.ui.util.LocalWindowLayout
import com.ehviewer.core.ui.util.NavigationChrome
import com.ehviewer.core.ui.util.ProvideVectorPainterCache
import com.ehviewer.core.ui.util.WindowLayout
import com.ehviewer.core.util.DesktopFileLog
import com.ehviewer.core.util.LogPriority
import com.ehviewer.core.util.logcat
import dev.icerock.moko.resources.compose.stringResource
import dev.icerock.moko.resources.desc.ResourceFormatted
import dev.icerock.moko.resources.desc.StringDesc
import java.awt.Window as AwtWindow
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 桌面壳骨架：单窗口页面栈（库 → 详情 → 阅读；设置）+ Miuix 主题三态 + 菜单栏 + 托盘驻留 + 尺寸记忆 + 会话恢复

// 悬浮底栏可见时页面内容的底部避让量（底栏胶囊高度 + 上下留白，对齐移动端 bottomBarPadding 88dp）
private val FLOATING_BAR_CONTENT_PADDING = 88.dp

private object EhViewerTrayPainter : Painter() {
    override val intrinsicSize: Size = Size(32f, 32f)
    override fun DrawScope.onDraw() {
        drawCircle(color = Color(0xFF00796B), radius = size.minDimension / 2)
    }
}

fun main() {
    // DPI 感知须在任何 AWT/Swing 类初始化前声明（设计文档 §4.7），否则高缩放显示器上内容被位图放大发虚
    DesktopDpi.install()
    val startupStartMs = System.currentTimeMillis()
    // 单实例锁：已有实例在运行时直接退出，防 DataStore/Room 多进程并发写坏数据
    if (!DesktopSingleInstance.tryAcquire()) {
        logcat("SingleInstance", LogPriority.WARN) { "ALREADY_RUNNING exit" }
        return
    }
    DesktopImageLoader.init()
    application {
        // 页面栈：库页为根，详情/阅读/设置逐层推入（纯模型 DesktopPageStack 驱动）
        var pages by remember {
            mutableStateOf(
                if (DesktopSettings.restoreSession.value) {
                    DesktopPageStack.restorePages(
                        DesktopPageStack.decodeSession(
                            DesktopSettings.sessionGalleries.value,
                            limit = DesktopPageStack.sanitizeRestoreLimit(DesktopSettings.restoreLimit.value),
                        ),
                    )
                } else {
                    DesktopPageStack.initial()
                },
            )
        }
        // 恢复尺寸钳入目标屏幕工作区（dp）：超屏尺寸会被 AWT 钳到最小值并使 windowState 与实际窗口脱钩
        val restoredPosition = rememberedWindowPosition()
        val restoredSize = run {
            val x = if (restoredPosition is androidx.compose.ui.window.WindowPosition.Absolute) restoredPosition.x.value.toInt() else Int.MIN_VALUE
            val (w, h) = DesktopWindowPlacement.clampWindowSizeToWorkArea(
                widthDp = DesktopSettings.windowWidth,
                heightDp = DesktopSettings.windowHeight,
                minWidthDp = DesktopLayoutPolicy.WINDOW_MIN_WIDTH_DP,
                minHeightDp = DesktopLayoutPolicy.WINDOW_MIN_HEIGHT_DP,
                workArea = screenWorkAreaDp(x, y = if (x == Int.MIN_VALUE) Int.MIN_VALUE else restoredPosition.y.value.toInt()),
            )
            androidx.compose.ui.unit.DpSize(w.dp, h.dp)
        }
        val windowState = rememberWindowState(
            size = restoredSize,
            position = restoredPosition,
        )
        var isWindowVisible by remember { mutableStateOf(true) }
        var focusRequestVersion by remember { mutableIntStateOf(0) }
        var showShortcutsHelp by remember { mutableStateOf(false) }
        var showAbout by remember { mutableStateOf(false) }
        var showOpenGalleryDialog by remember { mutableStateOf(false) }
        val themeMode by DesktopSettings.themeMode.valueFlow().collectAsState(DesktopSettings.themeMode.value)
        val closeToTray by DesktopSettings.closeToTray.valueFlow().collectAsState(DesktopSettings.closeToTray.value)
        val isTraySupported = remember { java.awt.SystemTray.isSupported() }

        fun showWindow() {
            isWindowVisible = true
            focusRequestVersion++
        }

        // 会话保存：页面栈变化即持久化详情链（含 hydrate 回写的标题）
        LaunchedEffect(Unit) {
            snapshotFlow { pages }
                .drop(1)
                .collect { stack ->
                    DesktopSettings.sessionGalleries.value = DesktopPageStack.encodeSession(
                        DesktopPageStack.detailSnapshot(stack),
                    )
                }
        }

        val openGallery: (BaseGalleryInfo) -> Unit = { gallery ->
            showWindow()
            pages = DesktopPageStack.push(pages, DesktopPage.GalleryDetail(gallery))
        }
        val openReader: (BaseGalleryInfo) -> Unit = { gallery ->
            pages = DesktopPageStack.push(pages, DesktopPage.Reader(gallery))
        }
        val popPage: () -> Unit = { pages = DesktopPageStack.pop(pages) }
        // 主库 Tab 状态上提：左侧导航栏与库页共用同一选中态（大屏导航外壳）
        var libraryTab by remember { mutableStateOf(LibraryTab.fromName(DesktopSettings.lastTab.value)) }
        val selectTab: (LibraryTab) -> Unit = { tab ->
            libraryTab = tab
            DesktopSettings.lastTab.value = tab.name
        }
        // 导航项统一入口：库 Tab 回到根并切换 Tab，设置项推入/关闭设置页
        val selectNavItem: (DesktopNavItem) -> Unit = { item ->
            val tab = item.toLibraryTab()
            if (tab != null) {
                pages = DesktopPageStack.popToRoot(pages)
                selectTab(tab)
            } else if (pages.last() is DesktopPage.Settings) {
                popPage()
            } else {
                pages = DesktopPageStack.push(pages, DesktopPage.Settings)
            }
        }

        fun hideOrExit() {
            val behavior = if (closeToTray) CloseBehavior.MINIMIZE_TO_TRAY else CloseBehavior.EXIT
            when (DesktopClosePolicy.evaluateClose(behavior = behavior, isTrayAvailable = isTraySupported)) {
                CloseDecision.HideToTray -> isWindowVisible = false
                CloseDecision.Exit -> exitApplication()
            }
        }

        if (isTraySupported) {
            Tray(
                icon = remember { EhViewerTrayPainter },
                tooltip = "EhViewer",
                onAction = ::showWindow,
                menu = {
                    Item(stringResource(MR.strings.tray_open_app), onClick = ::showWindow)
                    Item(stringResource(MR.strings.menu_settings), onClick = {
                        showWindow()
                        pages = DesktopPageStack.push(pages, DesktopPage.Settings)
                    })
                    Separator()
                    Item(stringResource(MR.strings.menu_exit), onClick = { exitApplication() })
                },
            )
        }

        if (isWindowVisible) {
            Window(
                onCloseRequest = ::hideOrExit,
                state = windowState,
                icon = remember { EhViewerTrayPainter },
                title = DesktopPageStack.pageTitle(
                    pages.last(),
                    untitledLabel = stringResource(MR.strings.desktop_untitled),
                    settingsLabel = stringResource(MR.strings.menu_settings),
                    readingLabel = stringResource(MR.strings.desktop_window_reading),
                    navItemsLabel = stringResource(MR.strings.desktop_nav_items_title),
                ),
                onKeyEvent = { event ->
                    val action = resolveKeyAction(
                        isKeyDown = event.type == KeyEventType.KeyDown,
                        isCtrlPressed = event.isCtrlPressed,
                        key = event.key,
                        hasSelection = false,
                        canCloseOnEscape = showShortcutsHelp ||
                            showOpenGalleryDialog ||
                            showAbout ||
                            windowState.placement == WindowPlacement.Fullscreen,
                    )
                    when (action) {
                        DesktopKeyAction.ClosePage -> {
                            when {
                                showAbout -> showAbout = false
                                showShortcutsHelp || showOpenGalleryDialog -> {
                                    showShortcutsHelp = false
                                    showOpenGalleryDialog = false
                                }
                                // 全屏态 Esc 惯例：先退出全屏而非返回
                                windowState.placement == WindowPlacement.Fullscreen ->
                                    windowState.placement = WindowPlacement.Floating
                                else -> popPage()
                            }
                            true
                        }
                        DesktopKeyAction.ExitApp -> {
                            exitApplication()
                            true
                        }
                        DesktopKeyAction.ToggleFullscreen -> {
                            windowState.placement = if (windowState.placement == WindowPlacement.Fullscreen) {
                                WindowPlacement.Floating
                            } else {
                                WindowPlacement.Fullscreen
                            }
                            true
                        }
                        DesktopKeyAction.ShowShortcutsHelp -> {
                            showShortcutsHelp = !showShortcutsHelp
                            true
                        }
                        else -> false
                    }
                },
            ) {
                val awtWindow: AwtWindow = this@Window.window
                // 最小可用尺寸：与宽屏断点对齐（MIUIX 规范 4.1），保证桌面恒为大屏形态
                awtWindow.minimumSize = java.awt.Dimension(
                    DesktopLayoutPolicy.WINDOW_MIN_WIDTH_DP,
                    DesktopLayoutPolicy.WINDOW_MIN_HEIGHT_DP,
                )
                SaveWindowSize(windowState)
                DisposableEffect(Unit) {
                    onDispose {
                        // 窗口隐藏/退出前兜底落盘（防抖窗口期内的最后一次变更不丢失）
                        saveWindowPlacement(windowState)
                    }
                }
                LaunchedEffect(focusRequestVersion) {
                    if (focusRequestVersion > 0) {
                        awtWindow.toFront()
                        awtWindow.requestFocus()
                    }
                }
                // 桌面拖拽惯例：浏览器链接等文本拖入窗口即解析打开画廊（任意页面均可），拖入期间显示放置高亮
                var dragHovering by remember { mutableStateOf(false) }
                DisposableEffect(awtWindow) {
                    val dropTarget = java.awt.dnd.DropTarget(
                        awtWindow,
                        object : java.awt.dnd.DropTargetListener {
                            override fun dragEnter(e: java.awt.dnd.DropTargetDragEvent) {
                                if (e.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor)) {
                                    dragHovering = true
                                }
                            }

                            override fun dragOver(e: java.awt.dnd.DropTargetDragEvent) {
                                if (e.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor)) {
                                    dragHovering = true
                                }
                            }

                            override fun dragExit(e: java.awt.dnd.DropTargetEvent) {
                                dragHovering = false
                            }

                            override fun dropActionChanged(e: java.awt.dnd.DropTargetDragEvent) {}

                            override fun drop(e: java.awt.dnd.DropTargetDropEvent) {
                                dragHovering = false
                                e.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY)
                                val text = runCatching {
                                    e.transferable.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor) as? String
                                }.getOrNull()
                                e.dropComplete(true)
                                val parsed = text?.let { DesktopOpenGalleryState.parseInput(it) } ?: return
                                openGallery(DesktopOpenGalleryState.createGalleryInfo(parsed))
                            }
                        },
                    )
                    onDispose {
                        dropTarget.setActive(false)
                    }
                }
                LaunchedEffect(Unit) {
                    logcat("Shell", LogPriority.INFO) {
                        "SHELL_STARTED width=${DesktopSettings.windowWidth} height=${DesktopSettings.windowHeight} " +
                            "elapsedMs=${System.currentTimeMillis() - startupStartMs}"
                    }
                }
                val darkTheme = when (themeMode) {
                    1 -> false
                    2 -> true
                    else -> isSystemInDarkTheme()
                }
                val amoled by DesktopSettings.blackDarkTheme.valueFlow().collectAsState(DesktopSettings.blackDarkTheme.value)
                // 与移动端同源的 Miuix 主题控制器（浅色 HyperOS 基底 / 深色支持 AMOLED 纯黑）
                val themeController = rememberMiuixThemeController(useDarkTheme = darkTheme, isAmoled = amoled)
                // 与移动端 Theme.setMiuixContent 同源：IconCached 消费点（RatingWidget 等）在缓存局部量缺失时直接崩溃
                ProvideVectorPainterCache {
                    // 应用内界面缩放兜底（设计文档 §4.7）：整体按密度缩放，文字/矢量按新密度重绘保持清晰
                    val baseDensity = LocalDensity.current
                    val uiScaleValue by DesktopSettings.uiScale.valueFlow().collectAsState(DesktopSettings.uiScale.value)
                    val scaledDensity = remember(baseDensity, uiScaleValue) {
                        Density(baseDensity.density * DesktopUiScale.factor(uiScaleValue), baseDensity.fontScale)
                    }
                    CompositionLocalProvider(LocalDensity provides scaledDensity) {
                        MiuixTheme(controller = themeController) {
                            // 滚动条接入 Miuix 配色：静息半透明轮廓色，悬停加深（桌面惯例）
                            val scrollbarStyle = scrollbarStyle(
                                thumbColor = MiuixTheme.colorScheme.outline.copy(alpha = 0.45f),
                                hoverThumbColor = MiuixTheme.colorScheme.outline.copy(alpha = 0.9f),
                            )
                            val backdrop: LayerBackdrop? = null
                            val navBarStyleValue by DesktopSettings.navBarStyle.valueFlow().collectAsState(DesktopSettings.navBarStyle.value)
                            val navBarStyle = DesktopNavBarStyle.fromValue(navBarStyleValue)
                            // 导航项来自可配置列表（设计文档 §4.5 / §6.6）；异常配置兜底为全显默认
                            val navItemsRaw by DesktopSettings.navItems.valueFlow().collectAsState(DesktopSettings.navItems.value)
                            val navItems = remember(navItemsRaw) {
                                DesktopNavItems.visible(DesktopNavItems.decode(navItemsRaw)).ifEmpty {
                                    DesktopNavItems.visible(DesktopNavItems.defaultConfig())
                                }
                            }
                            // 与移动端 MainActivity 同源：窗口判定统一走共享 WindowLayout，断点常量不再桌面自持
                            val windowLayout = WindowLayout(
                                widthDp = windowState.size.width.value.roundToInt(),
                                heightDp = windowState.size.height.value.roundToInt(),
                            )
                            CompositionLocalProvider(
                                LocalScrollbarStyle provides scrollbarStyle,
                                LocalBackdrop provides backdrop,
                                LocalWindowLayout provides windowLayout,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .blurBackdropSource(backdrop),
                                ) {
                                    val isReader = pages.last() is DesktopPage.Reader
                                    // 导航形态：设置强制优先，Auto 走共享宽高双门槛（矮窗口自动落回底栏）；阅读器全屏无外壳
                                    val chrome = when {
                                        isReader -> null
                                        navBarStyle == DesktopNavBarStyle.Rail -> NavigationChrome.Rail
                                        navBarStyle == DesktopNavBarStyle.FloatingBottomBar -> NavigationChrome.BottomBar
                                        else -> windowLayout.navigationChrome
                                    }
                                    if (chrome == null) {
                                        when (val page = pages.last()) {
                                            is DesktopPage.Reader -> ReaderScreen(gallery = page.gallery, onClose = popPage)
                                            else -> {}
                                        }
                                    } else {
                                        val shellDensity = LocalDensity.current.density
                                        LaunchedEffect(chrome, navBarStyle, windowLayout) {
                                            logcat("Shell", LogPriority.INFO) {
                                                "CHROME style=$navBarStyle chrome=$chrome window=${windowLayout.widthDp}x${windowLayout.heightDp}" +
                                                    " density=$shellDensity awt=${awtWindow.size.width}x${awtWindow.size.height}" +
                                                    " gc=${awtWindow.graphicsConfiguration.bounds}"
                                            }
                                        }
                                        // 与移动端 MainNavPolicy 同源的主项归并：Toplist→Whatshot、History→Downloads 进底栏
                                        val navPolicy = remember {
                                            NavPrimaryPolicy(
                                                primaryKeys = listOf(
                                                    DesktopNavItem.Home.name,
                                                    DesktopNavItem.Subscription.name,
                                                    DesktopNavItem.Whatshot.name,
                                                    DesktopNavItem.Favorites.name,
                                                    DesktopNavItem.Downloads.name,
                                                    DesktopNavItem.Settings.name,
                                                ),
                                                mergeAliases = mapOf(
                                                    DesktopNavItem.Toplist.name to DesktopNavItem.Whatshot.name,
                                                    DesktopNavItem.History.name to DesktopNavItem.Downloads.name,
                                                ),
                                            )
                                        }
                                        val navSpecs = navItems.map { item ->
                                            NavItemSpec(key = item.name, label = stringResource(item.titleRes), icon = navItemIcon(item))
                                        }
                                        val primarySpecs = navSpecs.toPrimaryItems(navPolicy)
                                        // 选中语义照抄移动端：库根页高亮当前 Tab，设置系页高亮设置项，二级页不选中
                                        val selectedKey = when (val page = pages.last()) {
                                            is DesktopPage.Library -> libraryTab.name
                                            is DesktopPage.Settings, is DesktopPage.NavItems -> DesktopNavItem.Settings.name
                                            else -> null
                                        }
                                        // 主级目的地（库页/设置页）才显示悬浮底栏，详情等二级页隐藏（移动端 isPrimaryDestination 语义）
                                        val bottomBarVisible = pages.last() is DesktopPage.Library || pages.last() is DesktopPage.Settings
                                        AppShell(
                                            items = navSpecs,
                                            primaryItems = primarySpecs,
                                            selectedIndex = navSpecs.indexOfKey(selectedKey),
                                            primarySelectedIndex = navPolicy.primaryIndexOf(selectedKey),
                                            chrome = chrome,
                                            bottomBarVisible = bottomBarVisible,
                                            backdrop = backdrop,
                                            onItemSelected = { index ->
                                                navSpecs.getOrNull(index)?.key?.let { key -> selectNavItem(DesktopNavItem.valueOf(key)) }
                                            },
                                            onPrimarySelected = { index ->
                                                primarySpecs.getOrNull(index)?.key?.let { key -> selectNavItem(DesktopNavItem.valueOf(key)) }
                                            },
                                        ) {
                                            // 悬浮底栏可见时页面内容整体避让（对齐移动端 bottomBarPadding 语义）；
                                            // 库页经共享搜索顶栏自行消费 LocalBottomBarContentPadding，不再整体垫高
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(
                                                        bottom = if (bottomBarVisible &&
                                                            chrome == NavigationChrome.BottomBar &&
                                                            pages.last() !is DesktopPage.Library
                                                        ) {
                                                            FLOATING_BAR_CONTENT_PADDING
                                                        } else {
                                                            0.dp
                                                        },
                                                    ),
                                            ) {
                                                when (val page = pages.last()) {
                                                    DesktopPage.Library -> LibraryScreen(
                                                        activeTab = libraryTab,
                                                        onTabSelect = selectTab,
                                                        openGalleryDialogVisible = showOpenGalleryDialog,
                                                        onOpenGalleryDialogOpen = { showOpenGalleryDialog = true },
                                                        onOpenGalleryDialogClose = { showOpenGalleryDialog = false },
                                                        onOpenGallery = openGallery,
                                                        onOpenReader = openReader,
                                                        onOpenSettings = {
                                                            if (pages.last() is DesktopPage.Settings) popPage() else pages = DesktopPageStack.push(pages, DesktopPage.Settings)
                                                        },
                                                        onShowShortcuts = { showShortcutsHelp = true },
                                                        onShowAbout = { showAbout = true },
                                                        onExit = { exitApplication() },
                                                    )
                                                    DesktopPage.Settings -> SettingsScreen(
                                                        onBack = popPage,
                                                        onShowShortcuts = { showShortcutsHelp = true },
                                                        onShowAbout = { showAbout = true },
                                                        onOpenNavItems = {
                                                            pages = DesktopPageStack.push(pages, DesktopPage.NavItems)
                                                        },
                                                    )
                                                    DesktopPage.NavItems -> NavItemsScreen(onBack = popPage)
                                                    is DesktopPage.GalleryDetail -> GalleryDetailPageContent(
                                                        gallery = page.gallery,
                                                        onBack = popPage,
                                                        onGalleryUpdated = { updated ->
                                                            // hydrate 回写：栈顶详情页替换为真实元数据，窗口标题随之更新
                                                            pages = DesktopPageStack.updateTopGallery(pages, updated)
                                                        },
                                                        onOpenReader = openReader,
                                                        onSearchTag = { tag ->
                                                            // 标签搜索：回到库页后驱动主库即时搜索（页面栈语义下的窗口内联动）
                                                            pages = DesktopPageStack.popToRoot(pages)
                                                            DesktopSearchBus.request(tag)
                                                            logcat("DetailPage", LogPriority.INFO) { "Tag search dispatched: $tag" }
                                                        },
                                                    )
                                                    is DesktopPage.Reader -> ReaderScreen(
                                                        gallery = page.gallery,
                                                        onClose = popPage,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (dragHovering) {
                                        // 拖拽放置高亮：全窗口主色描边 + 半透明衬底与居中提示胶囊（穿透点击，不拦截落点）
                                        val dropHintText = stringResource(MR.strings.desktop_drop_to_open)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.08f))
                                                .border(2.dp, MiuixTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                        )
                                        Box(
                                            modifier = Modifier.align(Alignment.Center),
                                        ) {
                                            Text(
                                                text = dropHintText,
                                                color = MiuixTheme.colorScheme.primary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier
                                                    .shadow(8.dp, SquircleShape(12.dp))
                                                    .clip(SquircleShape(12.dp))
                                                    .background(MiuixTheme.colorScheme.surface)
                                                    .border(1.dp, MiuixTheme.colorScheme.outline.copy(alpha = 0.15f), SquircleShape(12.dp))
                                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                            )
                                        }
                                    }
                                    if (showShortcutsHelp) {
                                        ShortcutsHelpDialog(onDismiss = { showShortcutsHelp = false })
                                    }
                                    if (showAbout) {
                                        AboutDialog(onDismiss = { showAbout = false })
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

@OptIn(FlowPreview::class)
@Composable
private fun SaveWindowSize(windowState: androidx.compose.ui.window.WindowState) {
    LaunchedEffect(windowState) {
        snapshotFlow { windowState.size }
            .drop(1)
            .debounce(500)
            .collect { size ->
                DesktopSettings.windowWidth = size.width.value.toInt()
                DesktopSettings.windowHeight = size.height.value.toInt()
            }
    }
    // 位置仅在浮动态记录（最大化/最小化的系统偏移坐标不落盘；PlatformDefault 的 x/y 非有限值同样排除；拔除显示器后残留坐标不落盘）
    LaunchedEffect(windowState) {
        snapshotFlow { windowState.position to windowState.placement }
            .drop(1)
            .debounce(500)
            .collect { (position, placement) ->
                persistWindowPosition(position, placement)
            }
    }
}

// 屏幕.bounds 枚举失败（headless 等异常环境）时返回空表 → 一律不更新位置，保持上次已知安全值
private fun visibleScreenBounds(): List<java.awt.Rectangle> = runCatching {
    java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
        .screenDevices.map { it.defaultConfiguration.bounds }
}.getOrDefault(emptyList())

// 浮动态且坐标有限且落在任一可见屏幕内才落盘；拖拽路径与退出保存路径共用同一守卫
private fun persistWindowPosition(position: androidx.compose.ui.window.WindowPosition, placement: WindowPlacement) {
    val x = position.x.value.toInt()
    val y = position.y.value.toInt()
    if (placement == WindowPlacement.Floating &&
        position.x.value.isFinite() &&
        position.y.value.isFinite() &&
        isPositionWithinScreens(x, y, visibleScreenBounds())
    ) {
        DesktopSettings.windowX = x
        DesktopSettings.windowY = y
    }
}

private fun saveWindowPlacement(windowState: androidx.compose.ui.window.WindowState) {
    DesktopSettings.windowWidth = windowState.size.width.value.toInt()
    DesktopSettings.windowHeight = windowState.size.height.value.toInt()
    persistWindowPosition(windowState.position, windowState.placement)
}

// 恢复记忆位置；坐标落在任何已接显示器边界外（如拔掉外接屏）时回退平台默认，防窗口飘出可达区域
@Composable
private fun rememberedWindowPosition(): androidx.compose.ui.window.WindowPosition {
    val x = DesktopSettings.windowX
    val y = DesktopSettings.windowY
    // 负数是左侧/上方显示器的合法坐标，仅以 Int.MIN_VALUE 哨兵判定「未记忆」
    return if (x != Int.MIN_VALUE && y != Int.MIN_VALUE && isReachableScreenPoint(x, y)) {
        androidx.compose.ui.window.WindowPosition(x.dp, y.dp)
    } else {
        androidx.compose.ui.window.WindowPosition.PlatformDefault
    }
}

// 目标屏幕工作区的 dp 尺寸：坐标命中哪块屏就取哪块（未记忆位置取主屏）。
// aware AWT 上报物理像素（defaultTransform=缩放），非 aware 上报逻辑像素（transform=1），
// 两种情形「上报值 / transform」均为 dp；枚举失败（headless 等异常环境）返回 null 由调用方跳过钳制
private fun screenWorkAreaDp(x: Int, y: Int): DesktopWindowPlacement.WorkArea? = runCatching {
    val ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
    val gc = ge.screenDevices
        .map { it.defaultConfiguration }
        .firstOrNull { x != Int.MIN_VALUE && y != Int.MIN_VALUE && it.bounds.contains(x, y) }
        ?: ge.defaultScreenDevice.defaultConfiguration
    val b = gc.bounds
    val insets = java.awt.Toolkit.getDefaultToolkit().getScreenInsets(gc)
    val scale = gc.defaultTransform.scaleX.takeIf { it > 0.0 } ?: 1.0
    DesktopWindowPlacement.WorkArea(
        width = ((b.width - insets.left - insets.right) / scale).roundToInt(),
        height = ((b.height - insets.top - insets.bottom) / scale).roundToInt(),
    )
}.getOrNull()

private fun isReachableScreenPoint(x: Int, y: Int): Boolean = runCatching {
    java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.any { device ->
        val b = device.defaultConfiguration.bounds
        x >= b.x - 200 && x < b.x + b.width && y >= b.y - 100 && y < b.y + b.height
    }
}.getOrDefault(false)

// 关于对话框：应用名 + 版本 + 仓库链接（桌面应用惯例的 Help/About 入口）
@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val versionText = stringResource(MR.strings.desktop_about_version, DESKTOP_VERSION)
    DesktopModalCard(
        title = stringResource(MR.strings.menu_about),
        onDismiss = onDismiss,
        cardWidth = 460.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "EhViewer-Miuix",
                color = MiuixTheme.colorScheme.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = versionText,
                color = MiuixTheme.colorScheme.onSurface,
                fontSize = 13.sp,
            )
            // 手动检查更新：行内三态反馈（检查中/已是最新/可更新直达 releases），失败明确提示不误报已最新
            val coroutineScope = rememberCoroutineScope()
            val checkLabel = stringResource(MR.strings.desktop_check_update)
            val checkingText = stringResource(MR.strings.desktop_update_checking)
            val upToDateText = stringResource(MR.strings.desktop_update_up_to_date)
            val unavailableText = stringResource(MR.strings.desktop_update_check_failed)
            var updateCheckState by remember { mutableStateOf<AboutUpdateCheckState?>(null) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DesktopHoverPill(
                    onClick = {
                        if (updateCheckState != AboutUpdateCheckState.Checking) {
                            coroutineScope.launch {
                                updateCheckState = AboutUpdateCheckState.Checking
                                updateCheckState = when (val result = checkLatestReleaseStatus(DESKTOP_VERSION)) {
                                    is UpdateCheckResult.Available -> AboutUpdateCheckState.Available(
                                        label = StringDesc.ResourceFormatted(MR.strings.desktop_update_available, result.info.tag).localized(),
                                        tag = result.info.tag,
                                    )
                                    UpdateCheckResult.UpToDate -> AboutUpdateCheckState.UpToDate(upToDateText)
                                    UpdateCheckResult.Unavailable -> AboutUpdateCheckState.Unavailable(unavailableText)
                                }
                            }
                        }
                    },
                    containerColor = MiuixTheme.colorScheme.surfaceContainerHighest,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = checkLabel,
                        color = MiuixTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                when (val state = updateCheckState) {
                    is AboutUpdateCheckState.Available -> Text(
                        text = state.label,
                        color = MiuixTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable { DesktopBrowser.openUrl(RELEASES_PAGE_URL) },
                    )
                    is AboutUpdateCheckState.UpToDate -> Text(
                        text = state.label,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = 12.sp,
                    )
                    is AboutUpdateCheckState.Unavailable -> Text(
                        text = state.label,
                        color = MiuixTheme.colorScheme.error,
                        fontSize = 12.sp,
                    )
                    AboutUpdateCheckState.Checking -> Text(
                        text = checkingText,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = 12.sp,
                    )
                    null -> {}
                }
            }
            val releasesUrl = RELEASES_PAGE_URL
            DesktopHoverPill(
                onClick = { DesktopBrowser.openUrl(releasesUrl) },
                containerColor = MiuixTheme.colorScheme.surfaceContainerHighest,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = releasesUrl,
                    color = MiuixTheme.colorScheme.primary,
                    fontSize = 12.sp,
                )
            }
            val clipboard = LocalClipboardManager.current
            val logLabel = stringResource(MR.strings.desktop_about_log)
            val copiedText = stringResource(MR.strings.copied_to_clipboard)
            var copiedLog by remember { mutableStateOf(false) }
            DesktopFileLog.defaultFile()?.let { logPath ->
                DesktopHoverPill(
                    onClick = {
                        clipboard.setText(AnnotatedString(logPath.toString()))
                        copiedLog = true
                    },
                    containerColor = MiuixTheme.colorScheme.surfaceContainerHighest,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) { hovered ->
                    Text(
                        text = if (copiedLog) copiedText else "$logLabel: $logPath",
                        color = if (copiedLog || hovered) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortcutsHelpDialog(onDismiss: () -> Unit) {
    DesktopModalCard(
        title = stringResource(MR.strings.menu_keyboard_shortcuts),
        onDismiss = onDismiss,
        cardWidth = 480.dp,
    ) {
        Text(
            text = stringResource(MR.strings.pref_category_general),
            color = MiuixTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        DesktopShortcuts.defaultEntries().forEach { entry ->
            ShortcutEntryRow(entry)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
        Text(
            text = stringResource(MR.strings.desktop_shortcuts_reader_section),
            color = MiuixTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        DesktopShortcuts.readerEntries().forEach { entry ->
            ShortcutEntryRow(entry)
        }
    }
}

@Composable
private fun ShortcutEntryRow(entry: DesktopShortcutEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SquircleShape(6.dp))
            .padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(SquircleShape(6.dp))
                .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text(
                text = entry.keyCombination,
                color = MiuixTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = stringResource(entry.descriptionRes),
            color = MiuixTheme.colorScheme.onSurface,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun GalleryDetailPageContent(
    gallery: BaseGalleryInfo,
    onBack: (() -> Unit)? = null,
    onGalleryUpdated: ((BaseGalleryInfo) -> Unit)? = null,
    onOpenReader: ((BaseGalleryInfo) -> Unit)? = null,
    onSearchTag: ((String) -> Unit)? = null,
) {
    var notifications by remember { mutableStateOf<List<DesktopNotification>>(emptyList()) }
    val nextNotificationId = remember { AtomicLong(1L) }
    val clipboard = LocalClipboardManager.current
    var previewCoverUrl by remember { mutableStateOf<String?>(null) }
    var previewUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var isFavorite by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    // Ctrl+O 打开时是仅有 gid/token 的占位信息，hydrate 成功后整体替换触发重组。
    // 以 gallery 为 key：同分支复用（会话恢复返回上一层详情、详情页上拖拽打开新画廊）时不得沿用上一画廊的元数据
    var currentGallery by remember(gallery) { mutableStateOf(gallery) }
    val addedToFavoriteText = stringResource(MR.strings.add_to_favorite_success)
    val removedFromFavoriteText = stringResource(MR.strings.remove_from_favorite_success)
    val noBrowserText = stringResource(MR.strings.no_browser_installed)
    val tagLabel = stringResource(MR.strings.search_sft)
    val metadataLoadingText = stringResource(MR.strings.desktop_gallery_metadata_loading)
    val metadataFailedText = stringResource(MR.strings.desktop_gallery_metadata_failed)
    val undoText = stringResource(MR.strings.desktop_notification_undo)

    fun showNotification(message: String) {
        val now = System.currentTimeMillis()
        notifications = DesktopNotificationManager.post(
            current = notifications,
            message = message,
            timestamp = now,
            idProvider = { nextNotificationId.getAndIncrement() },
        )
    }
    LaunchedEffect(gallery.gid) {
        isFavorite = withContext(Dispatchers.IO) {
            DesktopDatabase.eh.localFavoritesDao().contains(gallery.gid)
        }
    }

    LaunchedEffect(currentGallery.gid) {
        if (DesktopGalleryHydrator.needsHydration(currentGallery)) {
            val fetched = withContext(Dispatchers.IO) {
                DesktopGalleryHydrator.fetchInfo(currentGallery.gid, currentGallery.token)
            }
            if (fetched != null) {
                currentGallery = fetched
                // 回写页面栈：窗口标题随真实元数据更新
                onGalleryUpdated?.invoke(fetched)
                logcat("DetailPage", LogPriority.INFO) { "Hydrated gallery ${fetched.gid}" }
            } else {
                logcat("DetailPage", LogPriority.WARN) {
                    "Hydration unavailable ${currentGallery.gid} (offline / network error / parse failure)"
                }
                // 水合失败静默会让占位卡永久显示「加载中」，补可见反馈
                showNotification(metadataFailedText)
            }
        }
        // 记录阅读历史：HISTORY 表经 GID 与 GALLERIES 联表，须先确保画廊行存在（HISTORY JOIN GALLERIES 语义）。
        // 仅以真实元数据入库（Android 侧同为详情加载成功才记录），元数据不可得（离线）时不落假数据。
        if (!DesktopGalleryHydrator.needsHydration(currentGallery)) {
            withContext(Dispatchers.IO) {
                runCatching {
                    DesktopDatabase.eh.galleryDao().upsert(DesktopFavoritesState.toGalleryEntity(currentGallery))
                    DesktopDatabase.eh.historyDao().upsert(HistoryInfo(currentGallery.gid))
                }.onFailure {
                    logcat("DetailPage", LogPriority.WARN) { "History record failed ${currentGallery.gid}: $it" }
                }
            }
        }
    }

    AutoExpireNotifications(notifications) { notifications = it }

    // 收藏切换防重入：连点时忽略后续触发，避免基于过期 isFavorite 取反导致状态错乱
    var favoriteInFlight by remember { mutableStateOf(false) }
    fun toggleFavorite() {
        if (favoriteInFlight) return
        favoriteInFlight = true
        coroutineScope.launch {
            try {
                // 元数据未回填（离线等场景）时禁止把占位假数据写入画廊库/收藏
                if (DesktopGalleryHydrator.needsHydration(currentGallery)) {
                    showNotification(metadataLoadingText)
                    return@launch
                }
                val nextState = !isFavorite
                withContext(Dispatchers.IO) {
                    if (nextState) {
                        val entity = DesktopFavoritesState.toGalleryEntity(currentGallery)
                        DesktopDatabase.eh.galleryDao().upsert(entity)
                        DesktopDatabase.eh.localFavoritesDao().upsert(LocalFavoriteInfo(currentGallery.gid))
                    } else {
                        DesktopDatabase.eh.localFavoritesDao().deleteByKey(currentGallery.gid)
                    }
                }
                isFavorite = nextState
                if (nextState) {
                    showNotification(addedToFavoriteText)
                } else {
                    notifications = DesktopNotificationManager.post(
                        current = notifications,
                        message = removedFromFavoriteText,
                        timestamp = System.currentTimeMillis(),
                        idProvider = { nextNotificationId.getAndIncrement() },
                        actionLabel = undoText,
                        onAction = {
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    DesktopDatabase.eh.localFavoritesDao().upsert(LocalFavoriteInfo(currentGallery.gid))
                                }
                                isFavorite = true
                            }
                        },
                    )
                }
            } finally {
                favoriteInFlight = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background),
    ) {
        BlurredBar(backdrop = LocalBackdrop.current) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MiuixTheme.colorScheme.surface.copy(alpha = 0.85f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (onBack != null) {
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
                    }
                    Text(
                        text = galleryDisplayTitle(currentGallery.title, currentGallery.gid),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = if (onBack != null) 4.dp else 0.dp, end = 8.dp),
                    )
                    DesktopHoverPill(
                        onClick = {
                            DesktopBrowser.openUrl(galleryWebUrl(currentGallery.gid, currentGallery.token))
                        },
                        shape = SquircleShape(8.dp),
                        containerColor = MiuixTheme.colorScheme.surfaceContainerHighest,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = stringResource(MR.strings.open_in_browser),
                            color = MiuixTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    if (onOpenReader != null) {
                        DesktopHoverPill(
                            onClick = { onOpenReader(currentGallery) },
                            shape = SquircleShape(8.dp),
                            containerColor = MiuixTheme.colorScheme.primary,
                            hoverOverlayColor = Color.White.copy(alpha = 0.12f),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = stringResource(MR.strings.menu_read),
                                color = MiuixTheme.colorScheme.onPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
                HorizontalDivider(color = MiuixTheme.colorScheme.outline.copy(alpha = 0.2f))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            GalleryDetailPane(
                gallery = currentGallery,
                isFavorite = isFavorite,
                onToggleFavorite = { toggleFavorite() },
                onCopy = { value, label ->
                    clipboard.setText(AnnotatedString(value))
                    showNotification(if (label.isBlank()) value else "$label: $value")
                    logcat("DetailPage", LogPriority.INFO) { "Copied $label" }
                },
                onOpenUrl = { url ->
                    if (!DesktopBrowser.openUrl(url)) {
                        showNotification(noBrowserText)
                    }
                },
                onSearchTag = { tag ->
                    clipboard.setText(AnnotatedString(tag))
                    showNotification("$tagLabel: $tag")
                    onSearchTag?.invoke(tag)
                },
                onPreviewCover = { url, list ->
                    previewCoverUrl = url
                    previewUrls = list
                },
                onOpenReader = onOpenReader?.let { opener -> { opener(currentGallery) } },
            )
            if (notifications.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    notifications.forEach { notice ->
                        DesktopNotificationBubble(
                            notice = notice,
                            onDismiss = {
                                notifications = DesktopNotificationManager.dismiss(notifications, notice.id)
                            },
                        )
                    }
                }
            }

            previewCoverUrl?.let { coverUrl ->
                val currentIndex = previewUrls.indexOf(coverUrl)
                val hasPrev = currentIndex > 0
                val hasNext = currentIndex in 0 until (previewUrls.size - 1)
                CoverPreviewDialog(
                    imageUrl = coverUrl,
                    onDismiss = {
                        previewCoverUrl = null
                        previewUrls = emptyList()
                    },
                    onPrevious = if (hasPrev) {
                        { previewCoverUrl = previewUrls[currentIndex - 1] }
                    } else {
                        null
                    },
                    onNext = if (hasNext) {
                        { previewCoverUrl = previewUrls[currentIndex + 1] }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

// 关于弹窗手动检查更新的行内状态机：文案在协程回调（非组合语境）生成，经 StringDesc 本地化后携带
private sealed interface AboutUpdateCheckState {
    data object Checking : AboutUpdateCheckState
    data class UpToDate(val label: String) : AboutUpdateCheckState
    data class Available(val label: String, val tag: String) : AboutUpdateCheckState
    data class Unavailable(val label: String) : AboutUpdateCheckState
}

// 导航项图标映射：库 Tab 用 EhIcons，系统项用 MiuixIcons
private fun navItemIcon(item: DesktopNavItem): ImageVector = when (item) {
    DesktopNavItem.Home -> EhIcons.Filled.Home
    DesktopNavItem.Subscription -> EhIcons.Filled.Subscriptions
    DesktopNavItem.Whatshot -> EhIcons.Filled.Whatshot
    DesktopNavItem.Toplist -> EhIcons.Filled.FormatListNumbered
    DesktopNavItem.Favorites -> EhIcons.Filled.Bookmarks
    DesktopNavItem.History -> MiuixIcons.Recent
    DesktopNavItem.Downloads -> MiuixIcons.Download
    DesktopNavItem.Settings -> MiuixIcons.Settings
}
