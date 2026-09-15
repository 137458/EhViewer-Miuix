package com.hippo.ehviewer.ui

import com.hippo.ehviewer.client.EhUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CategoryFilterLogicTest {

    private fun isAllSelected(category: Int): Boolean =
        category == EhUtils.ALL_CATEGORY || category == EhUtils.NONE || category <= 0

    private fun isCategorySelected(currentCategory: Int, cat: Int): Boolean =
        !isAllSelected(currentCategory) && (currentCategory and cat != 0)

    private fun toggleCategory(currentCategory: Int, cat: Int): Int {
        val allSelected = isAllSelected(currentCategory)
        return if (isCategorySelected(currentCategory, cat)) {
            val newCat = currentCategory and cat.inv()
            if (newCat == 0) EhUtils.ALL_CATEGORY else newCat
        } else {
            if (allSelected) cat else (currentCategory or cat)
        }
    }

    @Test
    fun `isAllSelected returns true for ALL_CATEGORY, NONE and negative`() {
        assertTrue(isAllSelected(EhUtils.ALL_CATEGORY))
        assertTrue(isAllSelected(EhUtils.NONE))
        assertTrue(isAllSelected(-1))
        assertFalse(isAllSelected(EhUtils.DOUJINSHI))
        assertFalse(isAllSelected(EhUtils.MANGA or EhUtils.ARTIST_CG))
    }

    @Test
    fun `selecting category from ALL_CATEGORY isolates single category`() {
        val next = toggleCategory(EhUtils.ALL_CATEGORY, EhUtils.DOUJINSHI)
        assertEquals(EhUtils.DOUJINSHI, next)
        assertTrue(isCategorySelected(next, EhUtils.DOUJINSHI))
        assertFalse(isCategorySelected(next, EhUtils.MANGA))
        assertFalse(isAllSelected(next))
    }

    @Test
    fun `unselecting the only active category resets back to ALL_CATEGORY`() {
        val active = EhUtils.MANGA
        val next = toggleCategory(active, EhUtils.MANGA)
        assertEquals(EhUtils.ALL_CATEGORY, next)
        assertTrue(isAllSelected(next))
        assertFalse(isCategorySelected(next, EhUtils.MANGA))
    }

    @Test
    fun `multi-category selection retains all enabled categories`() {
        var current = toggleCategory(EhUtils.ALL_CATEGORY, EhUtils.DOUJINSHI)
        current = toggleCategory(current, EhUtils.MANGA)

        assertEquals(EhUtils.DOUJINSHI or EhUtils.MANGA, current)
        assertTrue(isCategorySelected(current, EhUtils.DOUJINSHI))
        assertTrue(isCategorySelected(current, EhUtils.MANGA))
        assertFalse(isCategorySelected(current, EhUtils.ARTIST_CG))
        assertFalse(isAllSelected(current))

        // Removing DOUJINSHI leaves MANGA
        current = toggleCategory(current, EhUtils.DOUJINSHI)
        assertEquals(EhUtils.MANGA, current)
        assertTrue(isCategorySelected(current, EhUtils.MANGA))
        assertFalse(isCategorySelected(current, EhUtils.DOUJINSHI))
    }
}
