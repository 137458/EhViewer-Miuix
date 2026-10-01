package com.ehviewer.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopClosePolicyTest {

    @Test
    fun evaluateCloseWithMultipleWindowsReducesCountWithoutExit() {
        val action = DesktopClosePolicy.evaluateClose(
            currentWindowCount = 3,
            behavior = CloseBehavior.EXIT,
            isTrayAvailable = true,
        )
        assertFalse(action.shouldExitApp)
        assertTrue(action.shouldKeepTray)
        assertEquals(2, action.remainingWindowsCount)
    }

    @Test
    fun evaluateCloseLastWindowWithExitBehaviorExitsApp() {
        val action = DesktopClosePolicy.evaluateClose(
            currentWindowCount = 1,
            behavior = CloseBehavior.EXIT,
            isTrayAvailable = true,
        )
        assertTrue(action.shouldExitApp)
        assertFalse(action.shouldKeepTray)
        assertEquals(0, action.remainingWindowsCount)
    }

    @Test
    fun evaluateCloseLastWindowWithMinimizeToTrayKeepsAppAliveWhenTrayAvailable() {
        val action = DesktopClosePolicy.evaluateClose(
            currentWindowCount = 1,
            behavior = CloseBehavior.MINIMIZE_TO_TRAY,
            isTrayAvailable = true,
        )
        assertFalse(action.shouldExitApp)
        assertTrue(action.shouldKeepTray)
        assertEquals(0, action.remainingWindowsCount)
    }

    @Test
    fun evaluateCloseLastWindowWithMinimizeToTrayFallsBackToExitWhenTrayUnavailable() {
        val action = DesktopClosePolicy.evaluateClose(
            currentWindowCount = 1,
            behavior = CloseBehavior.MINIMIZE_TO_TRAY,
            isTrayAvailable = false,
        )
        assertTrue(action.shouldExitApp)
        assertFalse(action.shouldKeepTray)
        assertEquals(0, action.remainingWindowsCount)
    }

    @Test
    fun evaluateCloseAlreadyZeroWindowsIsSafe() {
        val action = DesktopClosePolicy.evaluateClose(
            currentWindowCount = 0,
            behavior = CloseBehavior.EXIT,
            isTrayAvailable = false,
        )
        assertTrue(action.shouldExitApp)
        assertEquals(0, action.remainingWindowsCount)
    }
}
