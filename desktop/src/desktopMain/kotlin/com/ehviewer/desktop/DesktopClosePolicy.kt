package com.ehviewer.desktop

enum class CloseBehavior {
    EXIT,
    MINIMIZE_TO_TRAY,
}

sealed interface CloseDecision {
    data object Exit : CloseDecision
    data object HideToTray : CloseDecision
}

// 单窗口关闭决策：关窗按钮/Esc-在根页 的统一出口——配置托盘驻留且托盘可用则隐藏窗口，否则退出应用
object DesktopClosePolicy {
    fun evaluateClose(
        behavior: CloseBehavior,
        isTrayAvailable: Boolean,
    ): CloseDecision = if (behavior == CloseBehavior.MINIMIZE_TO_TRAY && isTrayAvailable) {
        CloseDecision.HideToTray
    } else {
        CloseDecision.Exit
    }
}
