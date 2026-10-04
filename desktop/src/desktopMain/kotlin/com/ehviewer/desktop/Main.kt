package com.ehviewer.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ehviewer.core.database.model.HistoryInfo
import com.ehviewer.core.database.model.LocalFavoriteInfo
import com.ehviewer.core.i18n.MR
import com.ehviewer.core.model.BaseGalleryInfo
import com.ehviewer.core.ui.component.SquircleShape
import com.ehviewer.core.ui.theme.rememberMiuixThemeController
import com.ehviewer.core.util.DesktopFileLog
import com.ehviewer.core.util.LogPriority
import com.ehviewer.core.util.logcat
import dev.icerock.moko.resources.compose.stringResource
import java.awt.Window as AwtWindow
import java.util.concurrent.atomic.AtomicLong
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
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 桌面壳骨架：单窗口页面栈（库 → 详情 → 阅读；设置）+ Miuix 主题三态 + 菜单栏 + 托盘驻留 + 尺寸记忆 + 会话恢复

private object EhViewerTrayPainter : Painter() {
    override val intrinsicSize: Size = Size(32f, 32f)
    override fun DrawScope.onDraw() {
        drawCircle(color = Color(0xFF00796B), radius = size.minDimension / 2)
    }
}

fun main() {
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
        val windowState = rememberWindowState(
            width = DesktopSettings.windowWidth.dp,
            height = DesktopSettings.windowHeight.dp,
            position = rememberedWindowPosition(),
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
                // 最小可用尺寸：主库布局在过小窗口下不可用
                awtWindow.minimumSize = java.awt.Dimension(560, 400)
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
                AppMenus(
                    onOpenSettings = {
                        pages = DesktopPageStack.push(pages, DesktopPage.Settings)
                    },
                    onShowShortcutsHelp = { showShortcutsHelp = true },
                    onShowAbout = { showAbout = true },
                    onExit = { exitApplication() },
                    onOpenGallery = { showOpenGalleryDialog = true },
                )
                // 桌面拖拽惯例：浏览器链接等文本拖入窗口即解析打开画廊（任意页面均可）
                DisposableEffect(awtWindow) {
                    val dropTarget = java.awt.dnd.DropTarget(
                        awtWindow,
                        object : java.awt.dnd.DropTargetListener {
                            override fun dragEnter(e: java.awt.dnd.DropTargetDragEvent) {}
                            override fun dragOver(e: java.awt.dnd.DropTargetDragEvent) {}
                            override fun dragExit(e: java.awt.dnd.DropTargetEvent) {}
                            override fun dropActionChanged(e: java.awt.dnd.DropTargetDragEvent) {}
                            override fun drop(e: java.awt.dnd.DropTargetDropEvent) {
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
                MiuixTheme(controller = themeController) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (val page = pages.last()) {
                            DesktopPage.Library -> LibraryScreen(
                                openGalleryDialogVisible = showOpenGalleryDialog,
                                onOpenGalleryDialogOpen = { showOpenGalleryDialog = true },
                                onOpenGalleryDialogClose = { showOpenGalleryDialog = false },
                                onOpenGallery = openGallery,
                                onOpenReader = openReader,
                            )
                            DesktopPage.Settings -> SettingsScreen(onBack = popPage)
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
    // 位置仅在浮动态记录（最大化/最小化的系统偏移坐标不落盘；PlatformDefault 的 x/y 非有限值同样排除）
    LaunchedEffect(windowState) {
        snapshotFlow { windowState.position to windowState.placement }
            .drop(1)
            .debounce(500)
            .collect { (position, placement) ->
                if (placement == WindowPlacement.Floating && position.x.value.isFinite() && position.y.value.isFinite()) {
                    DesktopSettings.windowX = position.x.value.toInt()
                    DesktopSettings.windowY = position.y.value.toInt()
                }
            }
    }
}

private fun saveWindowPlacement(windowState: androidx.compose.ui.window.WindowState) {
    DesktopSettings.windowWidth = windowState.size.width.value.toInt()
    DesktopSettings.windowHeight = windowState.size.height.value.toInt()
    if (windowState.placement == WindowPlacement.Floating &&
        windowState.position.x.value.isFinite() &&
        windowState.position.y.value.isFinite()
    ) {
        DesktopSettings.windowX = windowState.position.x.value.toInt()
        DesktopSettings.windowY = windowState.position.y.value.toInt()
    }
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

private fun isReachableScreenPoint(x: Int, y: Int): Boolean = runCatching {
    java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.any { device ->
        val b = device.defaultConfiguration.bounds
        x >= b.x - 200 && x < b.x + b.width && y >= b.y - 100 && y < b.y + b.height
    }
}.getOrDefault(false)

@Composable
private fun FrameWindowScope.AppMenus(
    onOpenSettings: () -> Unit,
    onShowShortcutsHelp: () -> Unit,
    onShowAbout: () -> Unit,
    onExit: () -> Unit,
    onOpenGallery: () -> Unit,
) = MenuBar {
    Menu(stringResource(MR.strings.menu_file)) {
        Item("${stringResource(MR.strings.desktop_open_gallery_title)}... (Ctrl+O)", onClick = onOpenGallery)
        Item(stringResource(MR.strings.menu_exit), onClick = onExit)
    }
    Menu(stringResource(MR.strings.menu_settings)) {
        Item(stringResource(MR.strings.menu_settings), onClick = onOpenSettings)
    }
    Menu(stringResource(MR.strings.menu_help)) {
        Item("${stringResource(MR.strings.menu_keyboard_shortcuts)} (F1)", onClick = onShowShortcutsHelp)
        Item(stringResource(MR.strings.menu_about), onClick = onShowAbout)
    }
}

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
            val releasesUrl = RELEASES_PAGE_URL
            Text(
                text = releasesUrl,
                color = MiuixTheme.colorScheme.primary,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(SquircleShape(6.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable { DesktopBrowser.openUrl(releasesUrl) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            val clipboard = LocalClipboardManager.current
            val logLabel = stringResource(MR.strings.desktop_about_log)
            val copiedText = stringResource(MR.strings.copied_to_clipboard)
            var copiedLog by remember { mutableStateOf(false) }
            DesktopFileLog.defaultFile()?.let { logPath ->
                Text(
                    text = if (copiedLog) copiedText else "$logLabel: $logPath",
                    color = if (copiedLog) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(SquircleShape(6.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable {
                            clipboard.setText(AnnotatedString(logPath.toString()))
                            copiedLog = true
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
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
        DesktopShortcuts.defaultEntries().forEach { entry ->
            ShortcutEntryRow(entry)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            text = stringResource(MR.strings.desktop_shortcuts_reader_section),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        DesktopShortcuts.readerEntries().forEach { entry ->
            ShortcutEntryRow(entry)
        }
    }
}

@Composable
private fun ShortcutEntryRow(entry: DesktopShortcutEntry) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
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
                fontWeight = FontWeight.Medium,
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
    var isFavorite by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    // Ctrl+O 打开时是仅有 gid/token 的占位信息，hydrate 成功后整体替换触发重组
    var currentGallery by remember { mutableStateOf(gallery) }
    val addedToFavoriteText = stringResource(MR.strings.add_to_favorite_success)
    val removedFromFavoriteText = stringResource(MR.strings.remove_from_favorite_success)
    val noBrowserText = stringResource(MR.strings.no_browser_installed)
    val tagLabel = stringResource(MR.strings.search_sft)
    val metadataLoadingText = stringResource(MR.strings.desktop_gallery_metadata_loading)
    val undoText = stringResource(MR.strings.desktop_notification_undo)

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

    fun showNotification(message: String) {
        val now = System.currentTimeMillis()
        notifications = DesktopNotificationManager.post(
            current = notifications,
            message = message,
            timestamp = now,
            idProvider = { nextNotificationId.getAndIncrement() },
        )
    }

    AutoExpireNotifications(notifications) { notifications = it }

    fun toggleFavorite() {
        coroutineScope.launch {
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
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                ) {
                    Icon(
                        imageVector = MiuixIcons.Back,
                        contentDescription = null,
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
                    .padding(start = if (onBack != null) 8.dp else 0.dp, end = 12.dp),
            )
            if (onOpenReader != null) {
                Box(
                    modifier = Modifier
                        .clip(SquircleShape(8.dp))
                        .background(MiuixTheme.colorScheme.primary)
                        .clickable { onOpenReader(currentGallery) }
                        .pointerHoverIcon(PointerIcon.Hand)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
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
                onPreviewCover = { url -> previewCoverUrl = url },
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
                        Box(
                            modifier = Modifier
                                .clip(SquircleShape(8.dp))
                                .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable {
                                    notifications = DesktopNotificationManager.dismiss(notifications, notice.id)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = notice.message,
                                    color = MiuixTheme.colorScheme.onSurface,
                                )
                                if (notice.actionLabel != null) {
                                    Text(
                                        text = notice.actionLabel,
                                        color = MiuixTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier
                                            .pointerHoverIcon(PointerIcon.Hand)
                                            .clickable {
                                                notice.onAction?.invoke()
                                                notifications = DesktopNotificationManager.dismiss(notifications, notice.id)
                                            },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            previewCoverUrl?.let { coverUrl ->
                CoverPreviewDialog(
                    imageUrl = coverUrl,
                    onDismiss = { previewCoverUrl = null },
                )
            }
        }
    }
}
