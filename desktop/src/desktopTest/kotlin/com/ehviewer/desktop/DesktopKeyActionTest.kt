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
    fun ctrlTabResolvesToCycleWindow() {
        val action = resolveKeyAction(
            isKeyDown = true,
            isCtrlPressed = true,
            key = Key.Tab,
        )
        assertEquals(DesktopKeyAction.CycleWindow, action)

        // KeyUp 忽略
        assertEquals(
            DesktopKeyAction.None,
            resolveKeyAction(isKeyDown = false, isCtrlPressed = true, key = Key.Tab),
        )
        // 无 Ctrl 的 Tab 不触发（避免劫持焦点导航）
        assertEquals(
            DesktopKeyAction.None,
            resolveKeyAction(isKeyDown = true, isCtrlPressed = false, key = Key.Tab),
        )
    }

    @Test
    fun cycleWindowIdRotatesThroughOpenWindows() {
        val ids = listOf(0L, 5L, 9L)
        // 依次向后轮转
        assertEquals(5L, cycleWindowId(ids, 0L))
        assertEquals(9L, cycleWindowId(ids, 5L))
        // 回绕到第一个
        assertEquals(0L, cycleWindowId(ids, 9L))
        // 单窗口或空列表：无需轮转
        assertEquals(null, cycleWindowId(listOf(3L), 3L))
        assertEquals(null, cycleWindowId(emptyList(), 3L))
        // 当前窗口不在列表中（已关闭）：回到第一个
        assertEquals(0L, cycleWindowId(ids, 42L))
    }

    @Test
    fun defaultEntriesContainCoreShortcuts() {
        val entries = DesktopShortcuts.defaultEntries()
        val combinations = entries.map { it.keyCombination }
        assertTrue(combinations.any { it.contains("Ctrl + W") || it.contains("Ctrl + Q") })
        assertTrue(combinations.any { it.contains("F5") || it.contains("Ctrl + R") })
        assertTrue(combinations.any { it.contains("Ctrl + O") })
        assertTrue(combinations.any { it.contains("Ctrl + Tab") })
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

    @Test
    fun directionKeysAndEnterResolveCorrectly() {
        val nextAction = resolveKeyAction(
            isKeyDown = true,
            isCtrlPressed = false,
            key = Key.DirectionDown,
        )
        assertEquals(DesktopKeyAction.SelectNext, nextAction)

        val prevAction = resolveKeyAction(
            isKeyDown = true,
            isCtrlPressed = false,
            key = Key.DirectionUp,
        )
        assertEquals(DesktopKeyAction.SelectPrevious, prevAction)

        val enterWithSelection = resolveKeyAction(
            isKeyDown = true,
            isCtrlPressed = false,
            key = Key.Enter,
            hasSelection = true,
        )
        assertEquals(DesktopKeyAction.OpenSelected, enterWithSelection)

        val enterWithoutSelection = resolveKeyAction(
            isKeyDown = true,
            isCtrlPressed = false,
            key = Key.Enter,
            hasSelection = false,
        )
        assertEquals(DesktopKeyAction.None, enterWithoutSelection)
    }

    @Test
    fun numPadEnterWithSelectionResolvesToOpenSelected() {
        val action = resolveKeyAction(
            isKeyDown = true,
            isCtrlPressed = false,
            key = Key.NumPadEnter,
            hasSelection = true,
        )
        assertEquals(DesktopKeyAction.OpenSelected, action)

        val actionNoSel = resolveKeyAction(
            isKeyDown = true,
            isCtrlPressed = false,
            key = Key.NumPadEnter,
            hasSelection = false,
        )
        assertEquals(DesktopKeyAction.None, actionNoSel)

        val actionKeyUp = resolveKeyAction(
            isKeyDown = false,
            isCtrlPressed = false,
            key = Key.NumPadEnter,
            hasSelection = true,
        )
        assertEquals(DesktopKeyAction.None, actionKeyUp)

        val actionCtrl = resolveKeyAction(
            isKeyDown = true,
            isCtrlPressed = true,
            key = Key.NumPadEnter,
            hasSelection = true,
        )
        assertEquals(DesktopKeyAction.None, actionCtrl)
    }

    @Test
    fun homeEndResolveToFirstAndLastSelection() {
        assertEquals(
            DesktopKeyAction.SelectFirst,
            resolveKeyAction(isKeyDown = true, isCtrlPressed = false, key = Key.MoveHome),
        )
        assertEquals(
            DesktopKeyAction.SelectLast,
            resolveKeyAction(isKeyDown = true, isCtrlPressed = false, key = Key.MoveEnd),
        )
        // KeyUp 忽略
        assertEquals(
            DesktopKeyAction.None,
            resolveKeyAction(isKeyDown = false, isCtrlPressed = false, key = Key.MoveHome),
        )
        // Ctrl+Home 不触发（保留系统语义）
        assertEquals(
            DesktopKeyAction.None,
            resolveKeyAction(isKeyDown = true, isCtrlPressed = true, key = Key.MoveHome),
        )
    }

    @Test
    fun desktopNavigationSelectsNextAndPrevious() {
        val g1 = com.ehviewer.core.model.BaseGalleryInfo(gid = 1L)
        val g2 = com.ehviewer.core.model.BaseGalleryInfo(gid = 2L)
        val g3 = com.ehviewer.core.model.BaseGalleryInfo(gid = 3L)
        val list = listOf(g1, g2, g3)

        // 未选中时按下：首项
        assertEquals(g1, DesktopNavigation.nextSelection(list, null))
        // 从 g1 下移：g2
        assertEquals(g2, DesktopNavigation.nextSelection(list, g1))
        // 从 g3 下移：停留在 g3（或不越界）
        assertEquals(g3, DesktopNavigation.nextSelection(list, g3))

        // 未选中时按上：末项
        assertEquals(g3, DesktopNavigation.previousSelection(list, null))
        // 从 g3 上移：g2
        assertEquals(g2, DesktopNavigation.previousSelection(list, g3))
        // 从 g1 上移：停留在 g1
        assertEquals(g1, DesktopNavigation.previousSelection(list, g1))

        // 空列表
        assertEquals(null, DesktopNavigation.nextSelection(emptyList(), null))
        assertEquals(null, DesktopNavigation.previousSelection(emptyList(), null))
    }
}
