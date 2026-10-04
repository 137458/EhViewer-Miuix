package com.ehviewer.desktop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.IntSize

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
    // 绝对跳转不经页码增量（调用方直取目标页），0 保证误用无害
    DesktopReaderNav.FirstPage, DesktopReaderNav.LastPage -> 0
}

// 阅读器导航边界判定：目标页 = 当前页 + 方向增量，落在 1..totalPages 方可翻页（增量随阅读方向反转）
fun canNavigateByDelta(page: Int, delta: Int, totalPages: Int): Boolean = page + delta in 1..totalPages

enum class DesktopReaderZoom {
    In,
    Out,
    Reset,
}

// 缩放键位：Ctrl+=/Ctrl+小键盘+ 放大、Ctrl+-/Ctrl+小键盘- 缩小、Ctrl+0/Ctrl+小键盘0 重置；无 Ctrl 不消费
fun resolveReaderZoom(isKeyDown: Boolean, isCtrlPressed: Boolean, key: Key): DesktopReaderZoom? {
    if (!isKeyDown || !isCtrlPressed) return null
    return when (key) {
        Key.Equals, Key.NumPadAdd -> DesktopReaderZoom.In
        Key.Minus, Key.NumPadSubtract -> DesktopReaderZoom.Out
        Key.Zero, Key.NumPad0 -> DesktopReaderZoom.Reset
        else -> null
    }
}

// 缩放态方向键平移：按键指示"想看哪边"（→ 内容左移），步长为视口 10%，钳制在 (scale-1)*viewport/2 内。
// 未缩放返回 null（方向键归翻页语义）；平移是空间操作，不随阅读方向反转。
fun pannedOffset(current: Offset, key: Key, scale: Float, viewport: IntSize): Offset? {
    if (scale <= 1f) return null
    val dx = when (key) {
        Key.DirectionLeft -> viewport.width * 0.1f
        Key.DirectionRight -> -viewport.width * 0.1f
        else -> 0f
    }
    val dy = when (key) {
        Key.DirectionUp -> viewport.height * 0.1f
        Key.DirectionDown -> -viewport.height * 0.1f
        else -> 0f
    }
    if (dx == 0f && dy == 0f) return null
    return clampReaderOffset(current + Offset(dx, dy), scale, viewport)
}

// 阅读器缩放不变量的钳制：scale 缩放的内容相对视口中心最大可平移 (scale-1)*viewport/2（原尺寸即零平移）。
// 零宽轴直接归规范 +0.0：coerceIn(-0f, 0f) 会产出 -0f，Offset 按位相等比较不认 -0f == 0f。
fun clampReaderOffset(offset: Offset, scale: Float, viewport: IntSize): Offset {
    val maxX = (scale - 1f) * viewport.width / 2f
    val maxY = (scale - 1f) * viewport.height / 2f
    return Offset(
        if (maxX <= 0f) 0f else offset.x.coerceIn(-maxX, maxX),
        if (maxY <= 0f) 0f else offset.y.coerceIn(-maxY, maxY),
    )
}

// 阅读器缩放契约：手势/键盘/双击共用同一边界；offset 恒受 clampReaderOffset 约束（含回到原尺寸即归零平移）。
const val READER_MIN_SCALE = 1f
const val READER_MAX_SCALE = 5f
const val READER_DOUBLE_TAP_SCALE = 2.5f

data class DesktopReaderZoomState(
    val scale: Float = READER_MIN_SCALE,
    val offset: Offset = Offset.Zero,
) {
    // 键盘缩放：步进/取整复用 DesktopZoomController，边界取阅读器契约，钳制收敛平移
    fun keyboardZoom(action: DesktopReaderZoom, viewport: IntSize): DesktopReaderZoomState = when (action) {
        DesktopReaderZoom.In -> copy(scale = DesktopZoomController.zoomIn(scale, maxScale = READER_MAX_SCALE))
        DesktopReaderZoom.Out -> copy(scale = DesktopZoomController.zoomOut(scale, minScale = READER_MIN_SCALE))
        DesktopReaderZoom.Reset -> DesktopReaderZoomState()
    }.clampedTo(viewport)

    // 手势变换：缩放系数与拖拽平移一次应用，边界与平移不变量统一钳制
    fun gestureZoom(zoomFactor: Float, pan: Offset, viewport: IntSize): DesktopReaderZoomState = copy(
        scale = (scale * zoomFactor).coerceIn(READER_MIN_SCALE, READER_MAX_SCALE),
        offset = offset + pan,
    ).clampedTo(viewport)

    // 双击在原尺寸与双击档位间切换（放大态双击即复位，平移随复位归零）
    fun doubleTapToggled(): DesktopReaderZoomState = if (scale > READER_MIN_SCALE) {
        DesktopReaderZoomState()
    } else {
        DesktopReaderZoomState(scale = READER_DOUBLE_TAP_SCALE)
    }

    // 键盘方向键平移：复用 pannedOffset 的步长与钳制；null 表示不消费（未缩放或非方向键）
    fun panned(key: Key, viewport: IntSize): DesktopReaderZoomState? = pannedOffset(offset, key, scale, viewport)?.let { copy(offset = it) }

    private fun clampedTo(viewport: IntSize): DesktopReaderZoomState = copy(offset = clampReaderOffset(offset, scale, viewport))
}

// 滚轮翻页累计器：越过阈值触发一次翻页并清零；向下滚（正 deltaY）= 下一页。
// 默认阈值 1：Compose Desktop 鼠标滚轮每格 scrollDelta = ±1.0（离散事件），一格即一页；
// 触控板为像素级小步流，需要平滑累计的调用方可显式传入更大的像素阈值。
class DesktopScrollPager(private val threshold: Float = 1f) {
    private var accumulated = 0f

    fun onDelta(deltaY: Float): Int? {
        accumulated += deltaY
        val step = when {
            accumulated >= threshold -> 1
            accumulated <= -threshold -> -1
            else -> 0
        }
        return if (step != 0) {
            accumulated = 0f
            step
        } else {
            null
        }
    }
}
