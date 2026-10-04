package com.ehviewer.desktop

import androidx.compose.ui.input.key.Key
import com.ehviewer.core.i18n.MR
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DesktopKeyActionTest {
    @Test
    fun defaultEntriesNotEmpty() {
        val entries = DesktopShortcuts.defaultEntries()
        assertTrue(entries.isNotEmpty())
    }

    @Test
    fun ctrlTabIsNotHijacked() {
        // 单窗口页面导航后 Ctrl+Tab 让位给焦点导航
        assertEquals(
            DesktopKeyAction.None,
            resolveKeyAction(isKeyDown = true, isCtrlPressed = true, key = Key.Tab),
        )
        assertEquals(
            DesktopKeyAction.None,
            resolveKeyAction(isKeyDown = true, isCtrlPressed = false, key = Key.Tab),
        )
    }

    @Test
    fun defaultEntriesContainCoreShortcuts() {
        val entries = DesktopShortcuts.defaultEntries()
        val combinations = entries.map { it.keyCombination }
        assertTrue(combinations.any { it.contains("Ctrl + W") })
        assertTrue(combinations.any { it.contains("Ctrl + Q") })
        assertTrue(combinations.any { it.contains("F5") || it.contains("Ctrl + R") })
        assertTrue(combinations.any { it.contains("Ctrl + O") })
        assertTrue(combinations.any { it.contains("Drag") })
        assertTrue(combinations.any { it.contains("Escape") })
        assertTrue(combinations.any { it.contains("F1") })
        // 多窗口轮转键位已随单窗口化移除
        assertTrue(entries.none { it.keyCombination.contains("Ctrl + Tab") })

        // 描述已资源化：两两不同（moko object 单例同一性，防复制粘贴错串）并抽查关键映射
        val descriptions = entries.map { it.descriptionRes }
        assertEquals(descriptions.size, descriptions.toSet().size)
        assertSame(MR.strings.shortcut_close_page, entries.first { it.keyCombination == "Ctrl + W" }.descriptionRes)
        assertSame(MR.strings.menu_exit, entries.first { it.keyCombination == "Ctrl + Q" }.descriptionRes)
        assertSame(MR.strings.shortcut_show_help, entries.first { it.keyCombination == "F1" }.descriptionRes)
        assertSame(MR.strings.shortcut_open_gallery_by_link, entries.first { it.keyCombination == "Ctrl + O" }.descriptionRes)
    }

    @Test
    fun ctrlQResolvesToExitApp() {
        val action = resolveKeyAction(
            isKeyDown = true,
            isCtrlPressed = true,
            key = Key.Q,
            hasSelection = false,
        )
        assertEquals(DesktopKeyAction.ExitApp, action)
    }

    @Test
    fun ctrlWResolvesToClosePage() {
        val action = resolveKeyAction(
            isKeyDown = true,
            isCtrlPressed = true,
            key = Key.W,
            hasSelection = false,
        )
        assertEquals(DesktopKeyAction.ClosePage, action)
    }

    @Test
    fun escapeWithCanCloseOnEscapeResolvesToClosePage() {
        val action = resolveKeyAction(
            isKeyDown = true,
            isCtrlPressed = false,
            key = Key.Escape,
            hasSelection = false,
            canCloseOnEscape = true,
        )
        assertEquals(DesktopKeyAction.ClosePage, action)

        val actionWithBoth = resolveKeyAction(
            isKeyDown = true,
            isCtrlPressed = false,
            key = Key.Escape,
            hasSelection = true,
            canCloseOnEscape = true,
        )
        assertEquals(DesktopKeyAction.ClosePage, actionWithBoth)

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

    @Test
    fun readerEntriesContainReaderPageKeys() {
        val entries = DesktopShortcuts.readerEntries()
        assertTrue(entries.isNotEmpty())
        // 相对翻页键位组合（随阅读方向反转）
        assertTrue(entries.any { it.keyCombination.contains("PageUp") && it.keyCombination.contains("Space") })
        // Home/End 独立条目、Escape 复用既有词条
        assertTrue(entries.any { it.keyCombination == "Home / End" })
        assertSame(MR.strings.shortcut_escape, entries.first { it.keyCombination == "Escape" }.descriptionRes)
    }

    @Test
    fun readerEntriesContainZoomKeys() {
        val entries = DesktopShortcuts.readerEntries()
        // 缩放键位条目：+/-/0 三组键同时出现
        val zoomEntry = entries.first { it.keyCombination.contains("Ctrl") && it.keyCombination.contains("0") }
        assertTrue(zoomEntry.keyCombination.contains("="))
        assertTrue(zoomEntry.keyCombination.contains("-"))
    }

    @Test
    fun readerEntriesExcludeLibraryOnlyKeys() {
        val entries = DesktopShortcuts.readerEntries()
        // 主库专属键位不得串入阅读分组
        assertTrue(entries.none { it.keyCombination.startsWith("Ctrl + W") })
        assertTrue(entries.none { it.keyCombination == "Ctrl + O" })
        assertTrue(entries.none { it.keyCombination.contains("Ctrl + Tab") })
    }

    @Test
    fun f11ResolvesToToggleFullscreen() {
        assertEquals(
            DesktopKeyAction.ToggleFullscreen,
            resolveKeyAction(isKeyDown = true, isCtrlPressed = false, key = Key.F11),
        )
        // KeyUp 忽略；Ctrl+F11 不消费
        assertEquals(
            DesktopKeyAction.None,
            resolveKeyAction(isKeyDown = false, isCtrlPressed = false, key = Key.F11),
        )
        assertEquals(
            DesktopKeyAction.None,
            resolveKeyAction(isKeyDown = true, isCtrlPressed = true, key = Key.F11),
        )
        // F11 条目进入主库快捷键指南
        assertTrue(DesktopShortcuts.defaultEntries().any { it.keyCombination == "F11" })
    }

    @Test
    fun defaultEntriesContainMultiSelectEntry() {
        // Ctrl+Click 批量多选与既有鼠标操作（双击/拖拽/右键）同录主库指南
        val entry = DesktopShortcuts.defaultEntries().firstOrNull { it.keyCombination == "Ctrl + Click" }
        assertTrue(entry != null)
        assertEquals(MR.strings.shortcut_multi_select, entry.descriptionRes)
    }

    @Test
    fun ctrlFResolvesToFocusSearch() {
        // Ctrl+F 聚焦库搜索框：KeyDown 消费，KeyUp 与无 Ctrl 的 F（输入字符场景）不消费
        assertEquals(
            DesktopKeyAction.FocusSearch,
            resolveKeyAction(isKeyDown = true, isCtrlPressed = true, key = Key.F),
        )
        assertEquals(
            DesktopKeyAction.None,
            resolveKeyAction(isKeyDown = false, isCtrlPressed = true, key = Key.F),
        )
        assertEquals(
            DesktopKeyAction.None,
            resolveKeyAction(isKeyDown = true, isCtrlPressed = false, key = Key.F),
        )
    }

    @Test
    fun defaultEntriesContainFocusSearchEntry() {
        // Ctrl+F 条目进入主库快捷键指南，描述与其他条目两两不同
        val entry = DesktopShortcuts.defaultEntries().firstOrNull { it.keyCombination == "Ctrl + F" }
        assertTrue(entry != null)
        assertSame(MR.strings.shortcut_focus_search, entry.descriptionRes)
    }
}
