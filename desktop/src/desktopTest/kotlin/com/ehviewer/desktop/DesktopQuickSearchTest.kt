package com.ehviewer.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopQuickSearchTest {

    @Test
    fun decodeBlankAndGarbageYieldEmptyList() {
        assertEquals(emptyList(), DesktopQuickSearch.decode(""))
        assertEquals(emptyList(), DesktopQuickSearch.decode("   "))
        assertEquals(emptyList(), DesktopQuickSearch.decode("not json at all"))
    }

    @Test
    fun encodeDecodeRoundTripPreservesItemsAndOrder() {
        val list = listOf(
            DesktopQuickSearchItem(id = 1, name = "touhou", query = "touhou", position = 0),
            DesktopQuickSearchItem(id = 2, name = "東方 tag", query = "language:chinese touhou", position = 1),
        )
        val decoded = DesktopQuickSearch.decode(DesktopQuickSearch.encode(list))
        assertEquals(list, decoded)
    }

    @Test
    fun addAppendsItemWithIncrementedIdAndTailPosition() {
        val current = listOf(DesktopQuickSearchItem(id = 5, name = "a", query = "a", position = 0))
        val updated = DesktopQuickSearch.add(current, name = "b", query = "cat:manga")
        assertEquals(2, updated.size)
        val added = updated.last()
        assertEquals(6L, added.id)
        assertEquals("b", added.name)
        assertEquals("cat:manga", added.query)
        assertEquals(1, added.position)
    }

    @Test
    fun addOnEmptyListStartsAtIdOne() {
        val updated = DesktopQuickSearch.add(emptyList(), name = "first", query = "q")
        assertEquals(listOf(DesktopQuickSearchItem(id = 1, name = "first", query = "q", position = 0)), updated)
    }

    @Test
    fun removeDropsItemAndRepacksPositions() {
        val current = listOf(
            DesktopQuickSearchItem(id = 1, name = "a", query = "a", position = 0),
            DesktopQuickSearchItem(id = 2, name = "b", query = "b", position = 1),
            DesktopQuickSearchItem(id = 3, name = "c", query = "c", position = 2),
        )
        val updated = DesktopQuickSearch.remove(current, id = 2)
        assertEquals(
            listOf(
                DesktopQuickSearchItem(id = 1, name = "a", query = "a", position = 0),
                DesktopQuickSearchItem(id = 3, name = "c", query = "c", position = 1),
            ),
            updated,
        )
    }

    @Test
    fun moveReordersAndRepacksPositions() {
        val current = listOf(
            DesktopQuickSearchItem(id = 1, name = "a", query = "a", position = 0),
            DesktopQuickSearchItem(id = 2, name = "b", query = "b", position = 1),
            DesktopQuickSearchItem(id = 3, name = "c", query = "c", position = 2),
        )
        // 与移动端 reorderable 同语义：removeAt(from) 后 insert(to)
        val updated = DesktopQuickSearch.move(current, fromIndex = 2, toIndex = 0)
        assertEquals(listOf(3L, 1L, 2L), updated.map { it.id })
        assertEquals(listOf(0, 1, 2), updated.map { it.position })
    }

    @Test
    fun moveInvalidIndicesReturnOriginalList() {
        val current = listOf(DesktopQuickSearchItem(id = 1, name = "a", query = "a", position = 0))
        assertTrue(DesktopQuickSearch.move(current, fromIndex = -1, toIndex = 0) === current)
        assertTrue(DesktopQuickSearch.move(current, fromIndex = 0, toIndex = 5) === current)
    }

    @Test
    fun validateNameRejectsBlankAndDuplicate() {
        val current = listOf(DesktopQuickSearchItem(id = 1, name = "touhou", query = "touhou", position = 0))
        assertEquals(DesktopQuickSearchNameError.BLANK, DesktopQuickSearch.validateName(current, ""))
        assertEquals(DesktopQuickSearchNameError.BLANK, DesktopQuickSearch.validateName(current, "   "))
        assertEquals(DesktopQuickSearchNameError.DUPLICATE, DesktopQuickSearch.validateName(current, "touhou"))
        assertEquals(null, DesktopQuickSearch.validateName(current, "東方"))
    }
}
