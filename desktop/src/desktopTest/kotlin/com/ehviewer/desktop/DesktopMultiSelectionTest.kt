package com.ehviewer.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopMultiSelectionTest {
    @Test
    fun toggleAddsAndRemoves() {
        val selection = DesktopMultiSelection()
        // 空选时 toggle 即加入
        selection.toggle(1L)
        assertEquals(setOf(1L), selection.gids)
        assertTrue(selection.isActive)
        // 再 toggle 同一 gid 即移除
        selection.toggle(1L)
        assertEquals(emptySet(), selection.gids)
        assertFalse(selection.isActive)
    }

    @Test
    fun selectOnlyReplacesSelection() {
        val selection = DesktopMultiSelection()
        selection.toggle(1L)
        selection.selectOnly(2L)
        assertEquals(setOf(2L), selection.gids)
    }

    @Test
    fun clearEmptiesSelection() {
        val selection = DesktopMultiSelection()
        selection.toggle(1L)
        selection.toggle(2L)
        selection.clear()
        assertEquals(emptySet(), selection.gids)
        assertFalse(selection.isActive)
    }

    @Test
    fun removeDropsDeletedGid() {
        val selection = DesktopMultiSelection()
        selection.toggle(1L)
        selection.toggle(2L)
        // 删除行同步移出选择集，防止幽灵选择
        selection.remove(1L)
        assertEquals(setOf(2L), selection.gids)
    }
}
