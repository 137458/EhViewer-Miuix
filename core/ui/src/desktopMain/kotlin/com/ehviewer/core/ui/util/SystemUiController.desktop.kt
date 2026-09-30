package com.ehviewer.core.ui.util

import androidx.compose.runtime.Composable

@Composable
actual fun rememberSystemUiController(): SystemUiController = DesktopSystemUiController

// 桌面无系统栏，控制器保留空实现以复用共享界面逻辑
private object DesktopSystemUiController : SystemUiController {
    override var showTransientSystemBarsBySwipe: Boolean = false
    override var isStatusBarVisible: Boolean = true
    override var isNavigationBarVisible: Boolean = true
    override var statusBarDarkContentEnabled: Boolean = true
}
