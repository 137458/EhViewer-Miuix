package com.ehviewer.desktop

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopReaderKeysTest {
    @Test
    fun pagingKeysResolveToRelativeNav() {
        assertEquals(DesktopReaderNav.RelativeForward, resolveReaderNav(Key.PageDown))
        assertEquals(DesktopReaderNav.RelativeForward, resolveReaderNav(Key.Spacebar))
        assertEquals(DesktopReaderNav.RelativeBackward, resolveReaderNav(Key.PageUp))
    }

    @Test
    fun homeEndResolveToAbsoluteNav() {
        assertEquals(DesktopReaderNav.FirstPage, resolveReaderNav(Key.MoveHome))
        assertEquals(DesktopReaderNav.LastPage, resolveReaderNav(Key.MoveEnd))
    }

    @Test
    fun arrowAndEnterKeysNotConsumed() {
        // 边界：←/→/Enter 既有处理不归本映射，避免双重消费
        assertNull(resolveReaderNav(Key.DirectionLeft))
        assertNull(resolveReaderNav(Key.DirectionRight))
        assertNull(resolveReaderNav(Key.Enter))
        assertNull(resolveReaderNav(Key.NumPadEnter))
        assertNull(resolveReaderNav(Key.F5))
    }

    @Test
    fun relativeNavDeltaFollowsReadingDirection() {
        assertEquals(1, DesktopReadingDirection.LTR.pageDeltaForNav(DesktopReaderNav.RelativeForward))
        assertEquals(-1, DesktopReadingDirection.LTR.pageDeltaForNav(DesktopReaderNav.RelativeBackward))
        // RTL 日漫：PageDown/Space 为上一页
        assertEquals(-1, DesktopReadingDirection.RTL.pageDeltaForNav(DesktopReaderNav.RelativeForward))
        assertEquals(1, DesktopReadingDirection.RTL.pageDeltaForNav(DesktopReaderNav.RelativeBackward))
    }
}
