package com.ehviewer.desktop

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme

// 桌面壳骨架：接入 Miuix 主题并跟随系统深浅色，验证共享主题 token 在桌面可用；
// 功能接线（路由/网络/设置界面）由后续轮次逐步迁入。
fun main() = application {
    val windowState = rememberWindowState(width = 1280.dp, height = 800.dp)
    Window(onCloseRequest = ::exitApplication, state = windowState, title = "EhViewer") {
        val darkTheme = isSystemInDarkTheme()
        MiuixTheme(colors = if (darkTheme) miuixDarkColorScheme() else miuixLightColorScheme()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("EhViewer Desktop", color = MiuixTheme.colorScheme.primary)
            }
        }
    }
}
