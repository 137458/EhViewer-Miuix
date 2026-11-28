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

// macOS 的命令键是 Command(⌘)，Windows/Linux 是 Ctrl；两平台各自把本平台的键当"命令键"
internal fun isMacOsPlatform(osName: String = System.getProperty("os.name", "")): Boolean = osName.startsWith("Mac", ignoreCase = true)

// 单窗口页面导航键位：Esc/Ctrl(⌘)+W 关闭当前页（栈根不响应），Ctrl(⌘)+Q 退出应用，窗口级全屏/刷新/指南不变
fun resolveKeyAction(
    isKeyDown: Boolean,
    isCtrlPressed: Boolean,
    key: Key,
    hasSelection: Boolean = false,
    canCloseOnEscape: Boolean = false,
    isMetaPressed: Boolean = false,
    platformAcceptsMeta: Boolean = isMacOsPlatform(),
): DesktopKeyAction {
    if (!isKeyDown) return DesktopKeyAction.None
    // 命令键：macOS 上 Command 生效，其余平台只认 Ctrl（Windows 的 Win 键不参与快捷键）
    val commandPressed = isCtrlPressed || (platformAcceptsMeta && isMetaPressed)

    return when {
        commandPressed && key == Key.Q -> DesktopKeyAction.ExitApp
        commandPressed && key == Key.W -> DesktopKeyAction.ClosePage
        commandPressed && key == Key.R -> DesktopKeyAction.Refresh
        commandPressed && key == Key.O -> DesktopKeyAction.OpenLinkDialog
        commandPressed && key == Key.F -> DesktopKeyAction.FocusSearch
        commandPressed && key == Key.One -> DesktopKeyAction.SwitchTabHistory
        commandPressed && key == Key.Two -> DesktopKeyAction.SwitchTabFavorites
        commandPressed && key == Key.Three -> DesktopKeyAction.SwitchTabOnline
        !commandPressed && key == Key.F5 -> DesktopKeyAction.Refresh
        !commandPressed && key == Key.F11 -> DesktopKeyAction.ToggleFullscreen
        !commandPressed && key == Key.F1 -> DesktopKeyAction.ShowShortcutsHelp
        !commandPressed && key == Key.Escape && canCloseOnEscape -> DesktopKeyAction.ClosePage
        !commandPressed && key == Key.Escape && hasSelection -> DesktopKeyAction.ClearSelection
        !commandPressed && key == Key.DirectionDown -> DesktopKeyAction.SelectNext
        !commandPressed && key == Key.DirectionUp -> DesktopKeyAction.SelectPrevious
        !commandPressed && key == Key.MoveHome -> DesktopKeyAction.SelectFirst
        !commandPressed && key == Key.MoveEnd -> DesktopKeyAction.SelectLast
        !commandPressed && (key == Key.Enter || key == Key.NumPadEnter) && hasSelection -> DesktopKeyAction.OpenSelected
        else -> DesktopKeyAction.None
    }
}
