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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ehviewer.core.database.model.LocalFavoriteInfo
import com.ehviewer.core.i18n.MR
import com.ehviewer.core.util.LogPriority
import com.ehviewer.core.util.logcat
import dev.icerock.moko.resources.compose.stringResource
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
        val windows = remember { mutableStateListOf(ShellWindow(0)) }
        var nextWindowId by remember { mutableStateOf(1L) }
        val themeMode by DesktopSettings.themeMode.valueFlow().collectAsState(DesktopSettings.themeMode.value)
        val closeToTray by DesktopSettings.closeToTray.valueFlow().collectAsState(DesktopSettings.closeToTray.value)
        val isTraySupported = remember { java.awt.SystemTray.isSupported() }

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
                )
                var showShortcutsHelp by remember { mutableStateOf(false) }
                Window(
                    onCloseRequest = { handleClose(window) },
                    state = windowState,
                    title = DesktopWindowManager.windowTitle(window.kind),
                    onKeyEvent = { event ->
                        val action = resolveKeyAction(
                            isKeyDown = event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown,
                            isCtrlPressed = event.isCtrlPressed,
                            key = event.key,
                            hasSelection = false,
                            canCloseOnEscape = showShortcutsHelp || window.kind !is DesktopWindowKind.Library,
                        )
                        when (action) {
                            DesktopKeyAction.CloseWindow -> {
                                if (showShortcutsHelp) {
                                    showShortcutsHelp = false
                                    true
                                } else {
                                    handleClose(window)
                                    true
                                }
                            }
                            DesktopKeyAction.ShowShortcutsHelp -> {
                                showShortcutsHelp = !showShortcutsHelp
                                true
                            }
                            DesktopKeyAction.ClearSelection -> {
                                if (showShortcutsHelp) {
                                    showShortcutsHelp = false
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
                    AppMenus(
                        onNewWindow = { windows.add(ShellWindow(nextWindowId++)) },
                        onOpenSettings = { windows.add(ShellWindow(nextWindowId++, isSettings = true)) },
                        onShowShortcutsHelp = { showShortcutsHelp = true },
                        onExit = {
                            windows.clear()
                            exitApplication()
                        },
                    )
                    if (window.kind == DesktopWindowKind.Library) {
                        SaveWindowSize(windowState)
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
                                    onOpenGalleryInNewWindow = { gallery ->
                                        val (updated, _) = DesktopWindowManager.openOrFocusGallery(
                                            windows = windows,
                                            gallery = gallery,
                                            nextIdProvider = { nextWindowId++ },
                                        )
                                        if (updated.size > windows.size) {
                                            windows.add(updated.last())
                                        }
                                    },
                                )
                                is DesktopWindowKind.GalleryDetail -> GalleryDetailWindowContent(kind.gallery)
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
}

@Composable
private fun FrameWindowScope.AppMenus(
    onNewWindow: () -> Unit,
    onOpenSettings: () -> Unit,
    onShowShortcutsHelp: () -> Unit,
    onExit: () -> Unit,
) = MenuBar {
    Menu(stringResource(MR.strings.menu_file)) {
        Item(stringResource(MR.strings.menu_new_window), onClick = onNewWindow)
        Item(stringResource(MR.strings.menu_exit), onClick = onExit)
    }
    Menu(stringResource(MR.strings.menu_settings)) {
        Item(stringResource(MR.strings.menu_settings), onClick = onOpenSettings)
    }
    Menu("Help") {
        Item("Keyboard Shortcuts (F1)", onClick = onShowShortcutsHelp)
    }
}

@Composable
private fun ShortcutsHelpDialog(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MiuixTheme.colorScheme.surface)
                .clickable(enabled = false) {}
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Keyboard Shortcuts",
                    color = MiuixTheme.colorScheme.primary,
                )
                Text(
                    text = "✕",
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable(onClick = onDismiss),
                )
            }
            HorizontalDivider()
            DesktopShortcuts.defaultEntries().forEach { entry ->
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
                        text = entry.description,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun GalleryDetailWindowContent(gallery: com.ehviewer.core.model.BaseGalleryInfo) {
    var notifications by remember { mutableStateOf<List<DesktopNotification>>(emptyList()) }
    val nextNotificationId = remember { AtomicLong(1L) }
    val clipboard = LocalClipboardManager.current
    var previewCoverUrl by remember { mutableStateOf<String?>(null) }
    var isFavorite by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val addedToFavoriteText = stringResource(MR.strings.add_to_favorite_success)
    val removedFromFavoriteText = stringResource(MR.strings.remove_from_favorite_success)
    val noBrowserText = stringResource(MR.strings.no_browser_installed)
    val tagLabel = stringResource(MR.strings.search_sft)

    LaunchedEffect(gallery.gid) {
        isFavorite = withContext(Dispatchers.IO) {
            DesktopDatabase.eh.localFavoritesDao().contains(gallery.gid)
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
            val nextState = !isFavorite
            withContext(Dispatchers.IO) {
                if (nextState) {
                    val entity = DesktopFavoritesState.toGalleryEntity(gallery)
                    DesktopDatabase.eh.galleryDao().upsert(entity)
                    DesktopDatabase.eh.localFavoritesDao().upsert(LocalFavoriteInfo(gallery.gid))
                } else {
                    DesktopDatabase.eh.localFavoritesDao().deleteByKey(gallery.gid)
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
            gallery = gallery,
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
                showNotification("$tagLabel: $tag")
            },
            onPreviewCover = { url -> previewCoverUrl = url },
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
