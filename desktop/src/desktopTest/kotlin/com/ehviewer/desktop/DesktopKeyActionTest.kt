package com.ehviewer.desktop

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopKeyActionTest {
    @Test
    fun defaultEntriesNotEmpty() {
        val entries = DesktopShortcuts.defaultEntries()
        assertTrue(entries.isNotEmpty())
    }

    @Test
    fun defaultEntriesContainCoreShortcuts() {
        val entries = DesktopShortcuts.defaultEntries()
        val combinations = entries.map { it.keyCombination }
        assertTrue(combinations.any { it.contains("Ctrl + W") || it.contains("Ctrl + Q") })
        assertTrue(combinations.any { it.contains("F5") || it.contains("Ctrl + R") })
        assertTrue(combinations.any { it.contains("Escape") })
        assertTrue(combinations.any { it.contains("F1") })

        entries.forEach { entry ->
            assertTrue(entry.keyCombination.isNotBlank())
            assertTrue(entry.description.isNotBlank())
        }
    }

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
    fun ctrlWResolvesToCloseWindow() {
        val action = resolveKeyAction(
            isKeyDown = true,
            isCtrlPressed = true,
            key = Key.W,
            hasSelection = false,
        )
        assertEquals(DesktopKeyAction.CloseWindow, action)
    }

    @Test
    fun escapeWithCanCloseOnEscapeResolvesToCloseWindow() {
        val action = resolveKeyAction(
            isKeyDown = true,
            isCtrlPressed = false,
            key = Key.Escape,
            hasSelection = false,
            canCloseOnEscape = true,
        )
        assertEquals(DesktopKeyAction.CloseWindow, action)

        val actionWithBoth = resolveKeyAction(
            isKeyDown = true,
            isCtrlPressed = false,
            key = Key.Escape,
            hasSelection = true,
            canCloseOnEscape = true,
        )
        assertEquals(DesktopKeyAction.CloseWindow, actionWithBoth)

        val actionClear = resolveKeyAction(
            isKeyDown = true,
            isCtrlPressed = false,
            key = Key.Escape,
            hasSelection = true,
            canCloseOnEscape = false,
        )
        assertEquals(DesktopKeyAction.ClearSelection, actionClear)
    }

    @Test
    fun f1ResolvesToShowShortcutsHelp() {
        val action = resolveKeyAction(
            isKeyDown = true,
            isCtrlPressed = false,
            key = Key.F1,
            hasSelection = false,
        )
        assertEquals(DesktopKeyAction.ShowShortcutsHelp, action)

        val actionWithSelection = resolveKeyAction(
            isKeyDown = true,
            isCtrlPressed = false,
            key = Key.F1,
            hasSelection = true,
        )
        assertEquals(DesktopKeyAction.ShowShortcutsHelp, actionWithSelection)

        val actionWithCtrl = resolveKeyAction(
            isKeyDown = true,
            isCtrlPressed = true,
            key = Key.F1,
            hasSelection = false,
        )
        assertEquals(DesktopKeyAction.None, actionWithCtrl)
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

        val actionW = resolveKeyAction(
            isKeyDown = false,
            isCtrlPressed = true,
            key = Key.W,
            hasSelection = true,
        )
        assertEquals(DesktopKeyAction.None, actionW)

        val actionEsc = resolveKeyAction(
            isKeyDown = false,
            isCtrlPressed = false,
            key = Key.Escape,
            canCloseOnEscape = true,
        )
        assertEquals(DesktopKeyAction.None, actionEsc)
    }
}
