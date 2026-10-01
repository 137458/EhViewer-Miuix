package com.ehviewer.desktop

import androidx.compose.ui.input.key.Key

enum class DesktopKeyAction {
    None,
    CloseWindow,
    Refresh,
    ClearSelection,
}

fun resolveKeyAction(
    isKeyDown: Boolean,
    isCtrlPressed: Boolean,
    key: Key,
    hasSelection: Boolean = false,
): DesktopKeyAction {
    if (!isKeyDown) return DesktopKeyAction.None

    return when {
        isCtrlPressed && key == Key.Q -> DesktopKeyAction.CloseWindow
        isCtrlPressed && key == Key.R -> DesktopKeyAction.Refresh
        !isCtrlPressed && key == Key.F5 -> DesktopKeyAction.Refresh
        !isCtrlPressed && key == Key.Escape && hasSelection -> DesktopKeyAction.ClearSelection
        else -> DesktopKeyAction.None
    }
}
