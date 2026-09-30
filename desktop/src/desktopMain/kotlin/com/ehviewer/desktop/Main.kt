package com.ehviewer.desktop

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ehviewer.core.i18n.MR
import com.ehviewer.core.util.LogPriority
import com.ehviewer.core.util.logcat
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme

// 桌面壳骨架：Miuix 主题跟随系统深浅色 + 平台菜单栏 + Ctrl+Q + 多窗口 + 尺寸记忆；
// 功能接线（路由/网络/设置界面）由后续轮次逐步迁入。

private data class ShellWindow(val id: Long, val isSettings: Boolean = false)

fun main() = application {
    val windows = remember { mutableStateListOf(ShellWindow(0)) }
    var nextWindowId by remember { mutableStateOf(1L) }
    val themeMode by DesktopSettings.themeMode.valueFlow().collectAsState(DesktopSettings.themeMode.value)

    for (window in windows) {
        // key 绑定窗口身份：多窗口下按位置记忆会让关窗时错关另一个原生窗口
        key(window.id) {
            val windowState = rememberWindowState(
                width = DesktopSettings.windowWidth.dp,
                height = DesktopSettings.windowHeight.dp,
            )
            Window(
                onCloseRequest = {
                    windows.remove(window)
                    if (windows.isEmpty()) {
                        exitApplication()
                    }
                },
                state = windowState,
                title = if (window.isSettings) "EhViewer Settings" else "EhViewer",
                onKeyEvent = { event ->
                    if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown &&
                        event.isCtrlPressed &&
                        event.key == Key.Q
                    ) {
                        windows.remove(window)
                        if (windows.isEmpty()) {
                            exitApplication()
                        }
                        true
                    } else {
                        false
                    }
                },
            ) {
                AppMenus(
                    onNewWindow = { windows.add(ShellWindow(nextWindowId++)) },
                    onOpenSettings = { windows.add(ShellWindow(nextWindowId++, isSettings = true)) },
                    onExit = {
                        windows.clear()
                        exitApplication()
                    },
                )
                if (!window.isSettings) {
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
                    val clipboard = LocalClipboardManager.current
                    ContextMenuArea(
                        items = {
                            listOf(
                                ContextMenuItem("Copy") {
                                    clipboard.setText(AnnotatedString("EhViewer Desktop"))
                                },
                            )
                        },
                    ) {
                        if (window.isSettings) {
                            SettingsScreen()
                        } else {
                            LibraryScreen()
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
private fun FrameWindowScope.AppMenus(onNewWindow: () -> Unit, onOpenSettings: () -> Unit, onExit: () -> Unit) = MenuBar {
    Menu(stringResource(MR.strings.menu_file)) {
        Item(stringResource(MR.strings.menu_new_window), onClick = onNewWindow)
        Item(stringResource(MR.strings.menu_exit), onClick = onExit)
    }
    Menu(stringResource(MR.strings.menu_settings)) {
        Item(stringResource(MR.strings.menu_settings), onClick = onOpenSettings)
    }
}
