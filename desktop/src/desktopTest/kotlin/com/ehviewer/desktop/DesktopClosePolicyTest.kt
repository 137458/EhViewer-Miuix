package com.ehviewer.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopClosePolicyTest {

    @Test
    fun exitBehaviorAlwaysExits() {
        assertEquals(
            CloseDecision.Exit,
            DesktopClosePolicy.evaluateClose(behavior = CloseBehavior.EXIT, isTrayAvailable = true),
        )
        assertEquals(
            CloseDecision.Exit,
            DesktopClosePolicy.evaluateClose(behavior = CloseBehavior.EXIT, isTrayAvailable = false),
        )
    }

    @Test
    fun minimizeToTrayHidesWindowWhenTrayAvailable() {
        assertEquals(
            CloseDecision.HideToTray,
            DesktopClosePolicy.evaluateClose(behavior = CloseBehavior.MINIMIZE_TO_TRAY, isTrayAvailable = true),
        )
    }

    @Test
    fun minimizeToTrayFallsBackToExitWhenTrayUnavailable() {
        assertEquals(
            CloseDecision.Exit,
            DesktopClosePolicy.evaluateClose(behavior = CloseBehavior.MINIMIZE_TO_TRAY, isTrayAvailable = false),
        )
    }
}
