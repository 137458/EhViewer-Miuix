package com.ehviewer.desktop

import java.awt.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopWindowPlacementTest {

    private val screens = listOf(
        Rectangle(0, 0, 1920, 1080),
        Rectangle(1920, 0, 1920, 1080),
    )

    @Test
    fun positionOnPrimaryScreenIsVisible() {
        assertTrue(isPositionWithinScreens(100, 100, screens))
    }

    @Test
    fun positionOnSecondaryScreenIsVisible() {
        assertTrue(isPositionWithinScreens(2000, 500, screens))
    }

    @Test
    fun positionBeyondAllScreensIsRejected() {
        // 外接显示器拔除后残留的旧坐标（如 4000,500）不可见
        assertFalse(isPositionWithinScreens(4000, 500, screens))
    }

    @Test
    fun negativeOffscreenPositionIsRejected() {
        assertFalse(isPositionWithinScreens(-2000, 100, screens))
    }

    @Test
    fun emptyScreenListRejectsAnyPosition() {
        assertFalse(isPositionWithinScreens(100, 100, emptyList()))
    }

    // —— 窗口尺寸钳制：高 DPI 下超屏记忆尺寸会被 AWT 钳到最小尺寸造成内容裁切 ——

    private fun clamp(
        width: Int,
        height: Int,
        work: DesktopWindowPlacement.WorkArea?,
    ): Pair<Int, Int> = DesktopWindowPlacement.clampWindowSizeToWorkArea(
        widthDp = width,
        heightDp = height,
        minWidthDp = DesktopLayoutPolicy.WINDOW_MIN_WIDTH_DP,
        minHeightDp = DesktopLayoutPolicy.WINDOW_MIN_HEIGHT_DP,
        workArea = work,
    )

    @Test
    fun sizeWithinWorkAreaStaysUntouched() {
        val clamped = clamp(1280, 800, DesktopWindowPlacement.WorkArea(width = 1463, height = 874))
        assertEquals(1280, clamped.first)
        assertEquals(800, clamped.second)
    }

    @Test
    fun oversizedWidthAndHeightClampToWorkArea() {
        // 175% 缩放屏实测：记忆 1477x928 > 工作区 1463x874 → 钳入
        val clamped = clamp(1477, 928, DesktopWindowPlacement.WorkArea(width = 1463, height = 874))
        assertEquals(1463, clamped.first)
        assertEquals(874, clamped.second)
    }

    @Test
    fun clampNeverGoesBelowMinimumSize() {
        val clamped = clamp(1477, 928, DesktopWindowPlacement.WorkArea(width = 500, height = 400))
        assertEquals(DesktopLayoutPolicy.WINDOW_MIN_WIDTH_DP, clamped.first)
        assertEquals(DesktopLayoutPolicy.WINDOW_MIN_HEIGHT_DP, clamped.second)
    }

    @Test
    fun unknownWorkAreaLeavesSizeUnchanged() {
        // 屏幕枚举失败（headless 等异常环境）跳过钳制，维持既有行为
        val clamped = clamp(1477, 928, null)
        assertEquals(1477, clamped.first)
        assertEquals(928, clamped.second)
    }
}
