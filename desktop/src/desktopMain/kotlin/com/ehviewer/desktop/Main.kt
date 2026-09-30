package com.ehviewer.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

// 桌面壳骨架：仅验证共享核心(核心模块/主题库/i18n/数据层)在 JVM 可用，
// 功能接线（主题/路由/网络）由后续轮次逐步迁入。
fun main() = application {
    val windowState = rememberWindowState(width = 1280.dp, height = 800.dp)
    Window(onCloseRequest = ::exitApplication, state = windowState, title = "EhViewer") {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("EhViewer Desktop")
        }
    }
}
