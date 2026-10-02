package com.ehviewer.desktop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.IntSize
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

    @Test
    fun ctrlZoomKeysResolveToZoomActions() {
        assertEquals(DesktopReaderZoom.In, resolveReaderZoom(isKeyDown = true, isCtrlPressed = true, key = Key.Equals))
        assertEquals(DesktopReaderZoom.In, resolveReaderZoom(isKeyDown = true, isCtrlPressed = true, key = Key.NumPadAdd))
        assertEquals(DesktopReaderZoom.Out, resolveReaderZoom(isKeyDown = true, isCtrlPressed = true, key = Key.Minus))
        assertEquals(DesktopReaderZoom.Out, resolveReaderZoom(isKeyDown = true, isCtrlPressed = true, key = Key.NumPadSubtract))
        assertEquals(DesktopReaderZoom.Reset, resolveReaderZoom(isKeyDown = true, isCtrlPressed = true, key = Key.Zero))
        assertEquals(DesktopReaderZoom.Reset, resolveReaderZoom(isKeyDown = true, isCtrlPressed = true, key = Key.NumPad0))
    }

    @Test
    fun zoomKeysRequireCtrlAndKeyDown() {
        // 无 Ctrl 不消费（保留单键语义）
        assertNull(resolveReaderZoom(isKeyDown = true, isCtrlPressed = false, key = Key.Equals))
        assertNull(resolveReaderZoom(isKeyDown = true, isCtrlPressed = false, key = Key.Minus))
        assertNull(resolveReaderZoom(isKeyDown = true, isCtrlPressed = false, key = Key.Zero))
        // KeyUp 忽略
        assertNull(resolveReaderZoom(isKeyDown = false, isCtrlPressed = true, key = Key.Equals))
        // 非缩放键不消费
        assertNull(resolveReaderZoom(isKeyDown = true, isCtrlPressed = true, key = Key.A))
        assertNull(resolveReaderZoom(isKeyDown = true, isCtrlPressed = true, key = Key.DirectionRight))
    }

    @Test
    fun directionKeysPanOnlyWhenZoomed() {
        val viewport = IntSize(1000, 800)
        // 未缩放：方向键归翻页语义，不平移
        assertNull(pannedOffset(Offset.Zero, Key.DirectionRight, scale = 1f, viewport = viewport))
        // 缩放态：→ 查看右侧内容（内容左移 = translationX 减小）
        val panned = pannedOffset(Offset.Zero, Key.DirectionRight, scale = 2f, viewport = viewport)
        assertEquals(-100f, panned!!.x)
        assertEquals(0f, panned.y)
        // ↑ 查看上方内容（内容下移 = translationY 增大）
        assertEquals(80f, pannedOffset(Offset.Zero, Key.DirectionUp, scale = 2f, viewport = viewport)!!.y)
    }

    @Test
    fun panClampsToZoomedBounds() {
        val viewport = IntSize(1000, 800)
        // 钳制上界 = (scale-1)*viewport/2
        val maxX = (2f - 1f) * 1000f / 2f
        val clamped = pannedOffset(Offset(maxX * 2f, 0f), Key.DirectionRight, scale = 2f, viewport = viewport)
        assertEquals(maxX, clamped!!.x)
        // 已在右边界，继续越界方向（← 内容右移）钳在边界；远离边界方向（→）合法移动
        assertEquals(maxX, pannedOffset(clamped, Key.DirectionLeft, scale = 2f, viewport = viewport)!!.x)
        assertEquals(maxX - 100f, pannedOffset(clamped, Key.DirectionRight, scale = 2f, viewport = viewport)!!.x)
        // 非方向键不消费
        assertNull(pannedOffset(Offset.Zero, Key.A, scale = 2f, viewport = viewport))
    }
}
