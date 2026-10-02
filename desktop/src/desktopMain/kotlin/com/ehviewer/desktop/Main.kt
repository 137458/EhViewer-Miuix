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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
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
import com.ehviewer.core.util.LogPriority
import com.ehviewer.core.util.logcat
import dev.icerock.moko.resources.compose.stringResource
import java.awt.Window as AwtWindow
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme

// 桌面壳骨架：Miuix 主题跟随系统深浅色 + 平台菜单栏 + Ctrl+Q + 多窗口 + 尺寸记忆 + 系统托盘与防误触

private object EhViewerTrayPainter : Painter() {
    override val intrinsicSize: Size = Size(32f, 32f)
    override fun DrawScope.onDraw() {
        drawCircle(color = Color(0xFF00796B), radius = size.minDimension / 2)
    }
}

fun main() {
    DesktopImageLoader.init()
    application {
        var nextWindowId by remember { mutableStateOf(1L) }
        val windows = remember {
            mutableStateListOf(ShellWindow(0)).apply {
                // 会话恢复（可在设置中关闭）：上次退出时打开的画廊窗口按原顺序重开（占位信息由 hydrator 回填）
                if (DesktopSettings.restoreSession.value) {
                    addAll(
                        DesktopWindowManager.restoreWindows(
                            DesktopWindowManager.decodeSession(DesktopSettings.sessionGalleries.value),
                        ) { nextWindowId++ },
                    )
                }
            }
        }
        val windowFrames = remember { mutableStateMapOf<Long, AwtWindow>() }
        val themeMode by DesktopSettings.themeMode.valueFlow().collectAsState(DesktopSettings.themeMode.value)
        val closeToTray by DesktopSettings.closeToTray.valueFlow().collectAsState(DesktopSettings.closeToTray.value)
        val isTraySupported = remember { java.awt.SystemTray.isSupported() }

        // 会话保存：窗口增删变化即持久化；列表为空（退出时 clear / 末窗关闭）跳过以保留最后会话
        LaunchedEffect(Unit) {
            snapshotFlow { windows.toList() }
                .drop(1)
                .collect { list ->
                    if (list.isNotEmpty()) {
                        DesktopSettings.sessionGalleries.value = DesktopWindowManager.encodeSession(
                            DesktopWindowManager.sessionSnapshot(list),
                        )
                    }
                }
        }

        // 开窗统一入口：多窗口防重 + 既有窗口置前（菜单/快捷键/拖拽共用）
        val openGalleryWindow: (com.ehviewer.core.model.BaseGalleryInfo) -> Unit = { gallery ->
            val (updated, windowId) = DesktopWindowManager.openOrFocusGallery(
                windows = windows,
                gallery = gallery,
                nextIdProvider = { nextWindowId++ },
            )
            if (updated.size > windows.size) {
                windows.add(updated.last())
            } else {
                windowFrames[windowId]?.toFront()
            }
        }

        // 阅读窗口入口：同 gid Reader 窗口防重
        val openReaderWindow: (com.ehviewer.core.model.BaseGalleryInfo) -> Unit = { gallery ->
            val (updated, windowId) = DesktopWindowManager.openOrFocusReader(
                windows = windows,
                gallery = gallery,
                nextIdProvider = { nextWindowId++ },
            )
            if (updated.size > windows.size) {
                windows.add(updated.last())
            } else {
                windowFrames[windowId]?.toFront()
            }
        }

        val handleClose: (ShellWindow) -> Unit = { targetWindow ->
            val behavior = if (closeToTray) CloseBehavior.MINIMIZE_TO_TRAY else CloseBehavior.EXIT
            val action = DesktopClosePolicy.evaluateClose(
                currentWindowCount = windows.size,
                behavior = behavior,
                isTrayAvailable = isTraySupported,
            )
            windows.remove(targetWindow)
            if (action.shouldExitApp) {
                exitApplication()
            }
        }

        if (isTraySupported) {
            Tray(
                icon = remember { EhViewerTrayPainter },
                tooltip = "EhViewer",
                onAction = {
                    if (windows.isEmpty()) {
                        windows.add(ShellWindow(nextWindowId++))
                    }
                },
                menu = {
                    Item(stringResource(MR.strings.tray_open_app), onClick = {
                        if (windows.isEmpty()) {
                            windows.add(ShellWindow(nextWindowId++))
                        }
                    })
                    Item(stringResource(MR.strings.menu_settings), onClick = {
                        windows.add(ShellWindow(nextWindowId++, isSettings = true))
                    })
                    Separator()
                    Item(stringResource(MR.strings.menu_exit), onClick = {
                        windows.clear()
                        exitApplication()
                    })
                },
            )
        }

        for (window in windows) {
            // key 绑定窗口身份：多窗口下按位置记忆会让关窗时错关另一个原生窗口
            key(window.id) {
                val windowState = rememberWindowState(
                    width = DesktopSettings.windowWidth.dp,
                    height = DesktopSettings.windowHeight.dp,
                    position = rememberedWindowPosition(),
                )
                var showShortcutsHelp by remember { mutableStateOf(false) }
                var showOpenGalleryDialog by remember { mutableStateOf(false) }
                Window(
                    onCloseRequest = { handleClose(window) },
                    state = windowState,
                    icon = remember { EhViewerTrayPainter },
                    title = DesktopWindowManager.windowTitle(
                        window.kind,
                        untitledLabel = stringResource(MR.strings.desktop_untitled),
                    ),
                    onKeyEvent = { event ->
                        val action = resolveKeyAction(
                            isKeyDown = event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown,
                            isCtrlPressed = event.isCtrlPressed,
                            isShiftPressed = event.isShiftPressed,
                            key = event.key,
                            hasSelection = false,
                            canCloseOnEscape = showShortcutsHelp ||
                                showOpenGalleryDialog ||
                                window.kind !is DesktopWindowKind.Library,
                        )
                        when (action) {
                            DesktopKeyAction.CloseWindow -> {
                                when {
                                    showShortcutsHelp || showOpenGalleryDialog -> {
                                        showShortcutsHelp = false
                                        showOpenGalleryDialog = false
                                    }
                                    // 全屏态 Esc 惯例：先退出全屏而非关窗
                                    windowState.placement == WindowPlacement.Fullscreen ->
                                        windowState.placement = WindowPlacement.Floating
                                    else -> handleClose(window)
                                }
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
                            DesktopKeyAction.CycleWindow -> {
                                cycleWindowId(windows.map { it.id }, window.id)?.let { targetId ->
                                    windowFrames[targetId]?.toFront()
                                }
                                true
                            }
                            DesktopKeyAction.CycleWindowBackward -> {
                                cycleWindowId(windows.map { it.id }, window.id, forward = false)?.let { targetId ->
                                    windowFrames[targetId]?.toFront()
                                }
                                true
                            }
                            DesktopKeyAction.ClearSelection -> {
                                if (showShortcutsHelp) {
                                    showShortcutsHelp = false
                                    true
                                } else if (showOpenGalleryDialog) {
                                    showOpenGalleryDialog = false
                                    true
                                } else if (window.kind is DesktopWindowKind.GalleryDetail) {
                                    handleClose(window)
                                    true
                                } else {
                                    false
                                }
                            }
                            else -> false
                        }
                    },
                ) {
                    // 循环变量 window(ShellWindow) 遮蔽 FrameWindowScope.window，需显式接收者取 AWT 原生窗口
                    val awtWindow: AwtWindow = this@Window.window
                    // 最小可用尺寸：主库双栏布局在过小窗口下不可用
                    awtWindow.minimumSize = java.awt.Dimension(560, 400)
                    DisposableEffect(window.id) {
                        windowFrames[window.id] = awtWindow
                        onDispose { windowFrames.remove(window.id) }
                    }
                    AppMenus(
                        onNewWindow = { windows.add(ShellWindow(nextWindowId++)) },
                        onOpenSettings = { windows.add(ShellWindow(nextWindowId++, isSettings = true)) },
                        onShowShortcutsHelp = { showShortcutsHelp = true },
                        onExit = {
                            windows.clear()
                            exitApplication()
                        },
                        showOpenGalleryItem = window.kind == DesktopWindowKind.Library,
                        onOpenGallery = { showOpenGalleryDialog = true },
                    )
                    if (window.kind == DesktopWindowKind.Library) {
                        SaveWindowSize(windowState)
                        // 桌面拖拽惯例：浏览器链接等文本拖入主库窗口即解析开窗（复用快捷打开解析）
                        DisposableEffect(window.id) {
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
                                        openGalleryWindow(DesktopOpenGalleryState.createGalleryInfo(parsed))
                                    }
                                },
                            )
                            onDispose {
                                dropTarget.setActive(false)
                            }
                        }
                        LaunchedEffect(Unit) {
                            logcat("Shell", LogPriority.INFO) {
                                "SHELL_STARTED width=${DesktopSettings.windowWidth} height=${DesktopSettings.windowHeight}"
                            }
                        }
                    }
                    val darkTheme = when (themeMode) {
                        1 -> false
                        2 -> true
                        else -> isSystemInDarkTheme()
                    }
                    MiuixTheme(colors = if (darkTheme) miuixDarkColorScheme() else miuixLightColorScheme()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            when (val kind = window.kind) {
                                DesktopWindowKind.Settings -> SettingsScreen()
                                DesktopWindowKind.Library -> LibraryScreen(
                                    openGalleryDialogVisible = showOpenGalleryDialog,
                                    onOpenGalleryDialogOpen = { showOpenGalleryDialog = true },
                                    onOpenGalleryDialogClose = { showOpenGalleryDialog = false },
                                    onOpenGalleryInNewWindow = { openGalleryWindow(it) },
                                    onOpenReader = { openReaderWindow(it) },
                                )
                                is DesktopWindowKind.GalleryDetail -> GalleryDetailWindowContent(
                                    gallery = kind.gallery,
                                    onOpenReader = { openReaderWindow(it) },
                                    onGalleryUpdated = { updated ->
                                        val idx = windows.indexOfFirst { it.id == window.id }
                                        if (idx >= 0) {
                                            val replaced = DesktopWindowManager.updateGalleryWindow(
                                                listOf(windows[idx]),
                                                window.id,
                                                updated,
                                            )
                                            windows[idx] = replaced.first()
                                        }
                                    },
                                )
                                is DesktopWindowKind.Reader -> ReaderScreen(
                                    gallery = kind.gallery,
                                    onClose = { handleClose(window) },
                                )
                            }
                            if (showShortcutsHelp) {
                                ShortcutsHelpDialog(onDismiss = { showShortcutsHelp = false })
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
    // 位置仅在浮动态记录（最大化/最小化的系统偏移坐标不落盘；PlatformDefault 的 x/y 非有限值同样排除）
    LaunchedEffect(windowState) {
        snapshotFlow { windowState.position to windowState.placement }
            .drop(1)
            .debounce(500)
            .collect { (position, placement) ->
                val x = position.x.value
                val y = position.y.value
                if (placement == androidx.compose.ui.window.WindowPlacement.Floating &&
                    x.isFinite() && y.isFinite()
                ) {
                    DesktopSettings.windowX = x.toInt()
                    DesktopSettings.windowY = y.toInt()
                }
            }
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
    onNewWindow: () -> Unit,
    onOpenSettings: () -> Unit,
    onShowShortcutsHelp: () -> Unit,
    onExit: () -> Unit,
    showOpenGalleryItem: Boolean = false,
    onOpenGallery: () -> Unit = {},
) = MenuBar {
    Menu(stringResource(MR.strings.menu_file)) {
        Item(stringResource(MR.strings.menu_new_window), onClick = onNewWindow)
        if (showOpenGalleryItem) {
            Item("${stringResource(MR.strings.desktop_open_gallery_title)}... (Ctrl+O)", onClick = onOpenGallery)
        }
        Item(stringResource(MR.strings.menu_exit), onClick = onExit)
    }
    Menu(stringResource(MR.strings.menu_settings)) {
        Item(stringResource(MR.strings.menu_settings), onClick = onOpenSettings)
    }
    Menu(stringResource(MR.strings.menu_help)) {
        Item("${stringResource(MR.strings.menu_keyboard_shortcuts)} (F1)", onClick = onShowShortcutsHelp)
    }
}

@Composable
private fun ShortcutsHelpDialog(onDismiss: () -> Unit) {
    DesktopModalCard(
        title = stringResource(MR.strings.menu_keyboard_shortcuts),
        onDismiss = onDismiss,
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = entry.keyCombination,
            color = MiuixTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(entry.descriptionRes),
            color = MiuixTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun GalleryDetailWindowContent(
    gallery: com.ehviewer.core.model.BaseGalleryInfo,
    onGalleryUpdated: ((com.ehviewer.core.model.BaseGalleryInfo) -> Unit)? = null,
    onOpenReader: ((com.ehviewer.core.model.BaseGalleryInfo) -> Unit)? = null,
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
                // 回写 windows 列表：原生窗口标题与多窗口防重判定随真实元数据更新
                onGalleryUpdated?.invoke(fetched)
                logcat("DetailWindow", LogPriority.INFO) { "Hydrated gallery ${fetched.gid}" }
            } else {
                logcat("DetailWindow", LogPriority.WARN) {
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
                    logcat("DetailWindow", LogPriority.WARN) { "History record failed ${currentGallery.gid}: $it" }
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

    LaunchedEffect(notifications) {
        if (notifications.isNotEmpty()) {
            delay(2500L)
            notifications = DesktopNotificationManager.expire(
                current = notifications,
                currentTime = System.currentTimeMillis(),
                ttlMs = 2500L,
            )
        }
    }

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
            showNotification(if (nextState) addedToFavoriteText else removedFromFavoriteText)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background),
    ) {
        GalleryDetailPane(
            gallery = currentGallery,
            isFavorite = isFavorite,
            onToggleFavorite = { toggleFavorite() },
            onCopy = { value, label ->
                clipboard.setText(AnnotatedString(value))
                showNotification(if (label.isBlank()) value else "$label: $value")
                logcat("DetailWindow", LogPriority.INFO) { "Copied $label" }
            },
            onOpenUrl = { url ->
                if (!DesktopBrowser.openUrl(url)) {
                    showNotification(noBrowserText)
                }
            },
            onSearchTag = { tag ->
                clipboard.setText(AnnotatedString(tag))
                // 双通道：复制到剪贴板 + 跨窗口搜索总线（主库 Online Tab 远程搜索 / 其余 Tab 本地过滤）
                DesktopSearchBus.request(tag)
                showNotification("$tagLabel: $tag")
            },
            onPreviewCover = { url -> previewCoverUrl = url },
            onOpenReader = onOpenReader?.let { opener -> { opener(gallery) } },
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable {
                                notifications = DesktopNotificationManager.dismiss(notifications, notice.id)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = notice.message,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
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
