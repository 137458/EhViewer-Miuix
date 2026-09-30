package com.ehviewer.desktop

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme

// 桌面壳骨架：Miuix 主题跟随系统深浅色 + 平台菜单栏 + Ctrl+Q + 多窗口 + 尺寸记忆；
// 功能接线（路由/网络/设置界面）由后续轮次逐步迁入。

private data class ShellWindow(val id: Long)

fun main() = application {
    val windows = remember { mutableStateListOf(ShellWindow(0)) }
    var nextWindowId by remember { mutableStateOf(1L) }

    windows.forEach { window ->
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
            title = "EhViewer",
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
                onExit = {
                    windows.clear()
                    exitApplication()
                },
            )
            SaveWindowSize(windowState)
            val darkTheme = isSystemInDarkTheme()
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
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("EhViewer Desktop", color = MiuixTheme.colorScheme.primary)
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
private fun FrameWindowScope.AppMenus(onNewWindow: () -> Unit, onExit: () -> Unit) = MenuBar {
    Menu(stringResource(MR.strings.menu_file)) {
        Item(stringResource(MR.strings.menu_new_window), onClick = onNewWindow)
        Item(stringResource(MR.strings.menu_exit), onClick = onExit)
    }
}
