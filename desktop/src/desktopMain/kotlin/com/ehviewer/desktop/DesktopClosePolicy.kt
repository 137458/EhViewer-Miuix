package com.ehviewer.desktop

enum class CloseBehavior {
    EXIT,
    MINIMIZE_TO_TRAY,
}

data class WindowManagerAction(
    val shouldExitApp: Boolean,
    val shouldKeepTray: Boolean,
    val remainingWindowsCount: Int,
)

object DesktopClosePolicy {
    fun evaluateClose(
        currentWindowCount: Int,
        behavior: CloseBehavior,
        isTrayAvailable: Boolean,
    ): WindowManagerAction {
        val remaining = maxOf(0, currentWindowCount - 1)
        return when {
            remaining > 0 -> WindowManagerAction(
                shouldExitApp = false,
                shouldKeepTray = isTrayAvailable,
                remainingWindowsCount = remaining,
            )
            behavior == CloseBehavior.MINIMIZE_TO_TRAY && isTrayAvailable -> WindowManagerAction(
                shouldExitApp = false,
                shouldKeepTray = true,
                remainingWindowsCount = 0,
            )
            else -> WindowManagerAction(
                shouldExitApp = true,
                shouldKeepTray = false,
                remainingWindowsCount = 0,
            )
        }
    }
}
