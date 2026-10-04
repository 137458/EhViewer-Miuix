package com.ehviewer.desktop

import androidx.compose.ui.input.key.Key

enum class DesktopKeyAction {
    None,
    ClosePage,
    ExitApp,
    Refresh,
    ClearSelection,
    ShowShortcutsHelp,
    SelectNext,
    SelectPrevious,
    SelectFirst,
    SelectLast,
    OpenSelected,
    OpenLinkDialog,
    ToggleFullscreen,
    FocusSearch,
    SwitchTabHistory,
    SwitchTabFavorites,
    SwitchTabOnline,
}

// 单窗口页面导航键位：Esc/Ctrl+W 关闭当前页（栈根不响应），Ctrl+Q 退出应用，窗口级全屏/刷新/指南不变
fun resolveKeyAction(
    isKeyDown: Boolean,
    isCtrlPressed: Boolean,
    key: Key,
    hasSelection: Boolean = false,
    canCloseOnEscape: Boolean = false,
): DesktopKeyAction {
    if (!isKeyDown) return DesktopKeyAction.None

    return when {
        isCtrlPressed && key == Key.Q -> DesktopKeyAction.ExitApp
        isCtrlPressed && key == Key.W -> DesktopKeyAction.ClosePage
        isCtrlPressed && key == Key.R -> DesktopKeyAction.Refresh
        isCtrlPressed && key == Key.O -> DesktopKeyAction.OpenLinkDialog
        isCtrlPressed && key == Key.F -> DesktopKeyAction.FocusSearch
        isCtrlPressed && key == Key.One -> DesktopKeyAction.SwitchTabHistory
        isCtrlPressed && key == Key.Two -> DesktopKeyAction.SwitchTabFavorites
        isCtrlPressed && key == Key.Three -> DesktopKeyAction.SwitchTabOnline
        !isCtrlPressed && key == Key.F5 -> DesktopKeyAction.Refresh
        !isCtrlPressed && key == Key.F11 -> DesktopKeyAction.ToggleFullscreen
        !isCtrlPressed && key == Key.F1 -> DesktopKeyAction.ShowShortcutsHelp
        !isCtrlPressed && key == Key.Escape && canCloseOnEscape -> DesktopKeyAction.ClosePage
        !isCtrlPressed && key == Key.Escape && hasSelection -> DesktopKeyAction.ClearSelection
        !isCtrlPressed && key == Key.DirectionDown -> DesktopKeyAction.SelectNext
        !isCtrlPressed && key == Key.DirectionUp -> DesktopKeyAction.SelectPrevious
        !isCtrlPressed && key == Key.MoveHome -> DesktopKeyAction.SelectFirst
        !isCtrlPressed && key == Key.MoveEnd -> DesktopKeyAction.SelectLast
        !isCtrlPressed && (key == Key.Enter || key == Key.NumPadEnter) && hasSelection -> DesktopKeyAction.OpenSelected
        else -> DesktopKeyAction.None
    }
}
