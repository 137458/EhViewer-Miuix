package com.ehviewer.desktop

import androidx.compose.ui.input.key.Key

enum class DesktopReaderNav {
    RelativeForward,
    RelativeBackward,
    FirstPage,
    LastPage,
}

// 阅读窗口补充键位：PageDown/Space/PageUp 相对翻页（随阅读方向反转），Home/End 恒跳首/末页。
// ←/→ 与 Enter 不归本映射（ReaderScreen 既有方向键翻页与跳页输入各自处理）。
fun resolveReaderNav(key: Key): DesktopReaderNav? = when (key) {
    Key.PageDown, Key.Spacebar -> DesktopReaderNav.RelativeForward
    Key.PageUp -> DesktopReaderNav.RelativeBackward
    Key.MoveHome -> DesktopReaderNav.FirstPage
    Key.MoveEnd -> DesktopReaderNav.LastPage
    else -> null
}

fun DesktopReadingDirection.pageDeltaForNav(nav: DesktopReaderNav): Int = when (nav) {
    DesktopReaderNav.RelativeForward -> pageDeltaForKey(forward = true)
    DesktopReaderNav.RelativeBackward -> pageDeltaForKey(forward = false)
    DesktopReaderNav.FirstPage -> -1
    DesktopReaderNav.LastPage -> 1
}
