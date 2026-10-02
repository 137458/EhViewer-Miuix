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

    @Test
    fun clampReaderOffsetEnforcesInvariant() {
        val viewport = IntSize(1000, 800)
        // 原尺寸：任何平移都归零
        assertEquals(Offset.Zero, clampReaderOffset(Offset(120f, -80f), scale = 1f, viewport = viewport))
        // 缩放态：界内保留、越界钳到 (scale-1)*viewport/2
        assertEquals(Offset(50f, -20f), clampReaderOffset(Offset(50f, -20f), scale = 2f, viewport = viewport))
        assertEquals(Offset(500f, -400f), clampReaderOffset(Offset(900f, -900f), scale = 2f, viewport = viewport))
    }

    @Test
    fun keyboardZoomRespectsReaderBounds() {
        val viewport = IntSize(1000, 800)
        // 常规：原尺寸放大一步到 1.25x，界内平移保留
        val zoomedIn = DesktopReaderZoomState(offset = Offset(50f, 20f)).keyboardZoom(DesktopReaderZoom.In, viewport)
        assertEquals(READER_MIN_SCALE + 0.25f, zoomedIn.scale)
        assertEquals(Offset(50f, 20f), zoomedIn.offset)
        // 边界：接近上限再放大钳在 5x；上限缩小一步回退
        assertEquals(READER_MAX_SCALE, DesktopReaderZoomState(scale = 4.9f).keyboardZoom(DesktopReaderZoom.In, viewport).scale)
        assertEquals(READER_MAX_SCALE, DesktopReaderZoomState(scale = READER_MAX_SCALE).keyboardZoom(DesktopReaderZoom.In, viewport).scale)
        assertEquals(4.75f, DesktopReaderZoomState(scale = READER_MAX_SCALE).keyboardZoom(DesktopReaderZoom.Out, viewport).scale)
    }

    @Test
    fun keyboardZoomOutStopsAtFitAndResetsPan() {
        val viewport = IntSize(1000, 800)
        // 缩小止于原尺寸，且回到原尺寸时平移归零
        val toFit = DesktopReaderZoomState(scale = 1.1f, offset = Offset(30f, 0f)).keyboardZoom(DesktopReaderZoom.Out, viewport)
        assertEquals(READER_MIN_SCALE, toFit.scale)
        assertEquals(Offset.Zero, toFit.offset)
        // 原尺寸再缩小保持原尺寸、平移为零
        val atFit = DesktopReaderZoomState().keyboardZoom(DesktopReaderZoom.Out, viewport)
        assertEquals(DesktopReaderZoomState(), atFit)
        // 未到原尺寸的中间态缩小：平移保留
        val mid = DesktopReaderZoomState(scale = 2.5f, offset = Offset(100f, 0f)).keyboardZoom(DesktopReaderZoom.Out, viewport)
        assertEquals(2.25f, mid.scale)
        assertEquals(Offset(100f, 0f), mid.offset)
    }

    @Test
    fun keyboardZoomResetRestoresFit() {
        val state = DesktopReaderZoomState(scale = 3f, offset = Offset(99f, 99f))
        assertEquals(DesktopReaderZoomState(), state.keyboardZoom(DesktopReaderZoom.Reset, IntSize(1000, 800)))
    }

    @Test
    fun gestureZoomAppliesFactorAndClampsToInvariant() {
        val viewport = IntSize(1000, 800)
        // 常规：2x 再放大 1.5 倍 → 3x，平移叠加且界内保留
        val zoomed = DesktopReaderZoomState(scale = 2f, offset = Offset(10f, 10f))
            .gestureZoom(zoomFactor = 1.5f, pan = Offset(40f, -10f), viewport = viewport)
        assertEquals(3f, zoomed.scale)
        assertEquals(Offset(50f, 0f), zoomed.offset)
        // 边界：放大系数过大钳在 5x，平移越界钳到 (5-1)*viewport/2
        val clamped = DesktopReaderZoomState(scale = 2f).gestureZoom(zoomFactor = 4f, pan = Offset(9999f, 0f), viewport = viewport)
        assertEquals(READER_MAX_SCALE, clamped.scale)
        assertEquals(2000f, clamped.offset.x)
        // 缩小触底回原尺寸：平移随之归零
        val toFit = DesktopReaderZoomState(scale = 1.5f, offset = Offset(100f, 0f))
            .gestureZoom(zoomFactor = 0.5f, pan = Offset.Zero, viewport = viewport)
        assertEquals(READER_MIN_SCALE, toFit.scale)
        assertEquals(Offset.Zero, toFit.offset)
    }

    @Test
    fun doubleTapTogglesFitAndPresetZoom() {
        // 原尺寸双击进入双击档位
        val tappedIn = DesktopReaderZoomState().doubleTapToggled()
        assertEquals(READER_DOUBLE_TAP_SCALE, tappedIn.scale)
        assertEquals(Offset.Zero, tappedIn.offset)
        // 双击档位与任意缩放态双击回原尺寸
        assertEquals(DesktopReaderZoomState(), DesktopReaderZoomState(scale = READER_DOUBLE_TAP_SCALE).doubleTapToggled())
        assertEquals(DesktopReaderZoomState(), DesktopReaderZoomState(scale = 3.2f, offset = Offset(5f, 5f)).doubleTapToggled())
    }

    @Test
    fun pannedDelegatesToPannedOffset() {
        val viewport = IntSize(1000, 800)
        // 未缩放不平移（不消费，归翻页语义）
        assertNull(DesktopReaderZoomState().panned(Key.DirectionRight, viewport))
        // 缩放态 → 复用同一平移步长与钳制
        val panned = DesktopReaderZoomState(scale = 2f).panned(Key.DirectionRight, viewport)
        assertEquals(-100f, panned!!.offset.x)
        // 非方向键不消费
        assertNull(DesktopReaderZoomState(scale = 2f).panned(Key.A, viewport))
    }

    @Test
    fun scrollPagerAccumulatesAndFiresOnThreshold() {
        val pager = DesktopScrollPager(threshold = 64f)
        // 小步滚动不触发
        assertNull(pager.onDelta(20f))
        assertNull(pager.onDelta(30f))
        // 累计越阈：向下滚 = 下一页
        assertEquals(1, pager.onDelta(20f))
        // 触发后清零重新累计
        assertNull(pager.onDelta(63f))
        assertEquals(-1, pager.onDelta(-64f))
    }

    @Test
    fun scrollPagerFiresOncePerLargeStep() {
        // 大步滚动一次只翻一页（不按步数放大）；方向随滚动方向（下=+1 下一页，上=-1 上一页）
        val pager = DesktopScrollPager(threshold = 64f)
        assertEquals(1, pager.onDelta(500f))
        assertEquals(-1, pager.onDelta(-500f))
    }
}
