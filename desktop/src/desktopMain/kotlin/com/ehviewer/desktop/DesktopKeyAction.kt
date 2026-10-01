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
    SelectFirst,
    SelectLast,
    OpenSelected,
    OpenLinkDialog,
    CycleWindow,
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
        isCtrlPressed && key == Key.Tab -> DesktopKeyAction.CycleWindow
        !isCtrlPressed && key == Key.F5 -> DesktopKeyAction.Refresh
        !isCtrlPressed && key == Key.F1 -> DesktopKeyAction.ShowShortcutsHelp
        !isCtrlPressed && key == Key.Escape && canCloseOnEscape -> DesktopKeyAction.CloseWindow
        !isCtrlPressed && key == Key.Escape && hasSelection -> DesktopKeyAction.ClearSelection
        !isCtrlPressed && key == Key.DirectionDown -> DesktopKeyAction.SelectNext
        !isCtrlPressed && key == Key.DirectionUp -> DesktopKeyAction.SelectPrevious
        !isCtrlPressed && key == Key.MoveHome -> DesktopKeyAction.SelectFirst
        !isCtrlPressed && key == Key.MoveEnd -> DesktopKeyAction.SelectLast
        !isCtrlPressed && (key == Key.Enter || key == Key.NumPadEnter) && hasSelection -> DesktopKeyAction.OpenSelected
        else -> DesktopKeyAction.None
    }
}

// Ctrl+Tab 轮转目标：按打开顺序的下一个窗口 id；单窗口/空列表无需轮转，未知当前窗口回第一个
fun cycleWindowId(ids: List<Long>, currentId: Long): Long? {
    if (ids.size <= 1) return null
    val index = ids.indexOf(currentId)
    if (index < 0) return ids.first()
    return ids[(index + 1) % ids.size]
}
