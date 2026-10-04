package com.ehviewer.desktop

import java.awt.Rectangle
import kotlin.test.Test
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
}
