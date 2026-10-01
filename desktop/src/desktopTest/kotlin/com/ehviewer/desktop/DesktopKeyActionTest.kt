package com.ehviewer.desktop

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopKeyActionTest {
    @Test
    fun ctrlQResolvesToCloseWindow() {
        val action = resolveKeyAction(
            isKeyDown = true,
            isCtrlPressed = true,
            key = Key.Q,
            hasSelection = false,
        )
        assertEquals(DesktopKeyAction.CloseWindow, action)
    }

    @Test
    fun ctrlRAndF5ResolveToRefresh() {
        val actionCtrlR = resolveKeyAction(
            isKeyDown = true,
            isCtrlPressed = true,
            key = Key.R,
            hasSelection = false,
        )
        assertEquals(DesktopKeyAction.Refresh, actionCtrlR)

        val actionF5 = resolveKeyAction(
            isKeyDown = true,
            isCtrlPressed = false,
            key = Key.F5,
            hasSelection = false,
        )
        assertEquals(DesktopKeyAction.Refresh, actionF5)
    }

    @Test
    fun escapeWithSelectionResolvesToClearSelection() {
        val actionWithSel = resolveKeyAction(
            isKeyDown = true,
            isCtrlPressed = false,
            key = Key.Escape,
            hasSelection = true,
        )
        assertEquals(DesktopKeyAction.ClearSelection, actionWithSel)

        val actionWithoutSel = resolveKeyAction(
            isKeyDown = true,
            isCtrlPressed = false,
            key = Key.Escape,
            hasSelection = false,
        )
        assertEquals(DesktopKeyAction.None, actionWithoutSel)
    }

    @Test
    fun keyUpAlwaysResolvesToNone() {
        val action = resolveKeyAction(
            isKeyDown = false,
            isCtrlPressed = true,
            key = Key.Q,
            hasSelection = true,
        )
        assertEquals(DesktopKeyAction.None, action)
    }
}
