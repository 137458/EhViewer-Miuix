package com.ehviewer.desktop

import androidx.compose.ui.input.key.Key

enum class DesktopKeyAction {
    None,
    CloseWindow,
    Refresh,
    ClearSelection,
    ShowShortcutsHelp,
    SelectNext,
    SelectPrevious,
    OpenSelected,
    OpenLinkDialog,
}

fun resolveKeyAction(
    isKeyDown: Boolean,
    isCtrlPressed: Boolean,
    key: Key,
    hasSelection: Boolean = false,
    canCloseOnEscape: Boolean = false,
): DesktopKeyAction {
    if (!isKeyDown) return DesktopKeyAction.None

    return when {
        isCtrlPressed && (key == Key.Q || key == Key.W) -> DesktopKeyAction.CloseWindow
        isCtrlPressed && key == Key.R -> DesktopKeyAction.Refresh
        isCtrlPressed && key == Key.O -> DesktopKeyAction.OpenLinkDialog
        !isCtrlPressed && key == Key.F5 -> DesktopKeyAction.Refresh
        !isCtrlPressed && key == Key.F1 -> DesktopKeyAction.ShowShortcutsHelp
        !isCtrlPressed && key == Key.Escape && canCloseOnEscape -> DesktopKeyAction.CloseWindow
        !isCtrlPressed && key == Key.Escape && hasSelection -> DesktopKeyAction.ClearSelection
        !isCtrlPressed && key == Key.DirectionDown -> DesktopKeyAction.SelectNext
        !isCtrlPressed && key == Key.DirectionUp -> DesktopKeyAction.SelectPrevious
        !isCtrlPressed && (key == Key.Enter || key == Key.NumPadEnter) && hasSelection -> DesktopKeyAction.OpenSelected
        else -> DesktopKeyAction.None
    }
}
