package com.ehviewer.desktop

import com.ehviewer.core.model.BaseGalleryInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopSortOrderTest {
    private fun createGallery(
        gid: Long,
        title: String? = "Gallery $gid",
        rating: Float = 0f,
        pages: Int = 0,
    ): BaseGalleryInfo = BaseGalleryInfo(
        gid = gid,
        title = title,
        rating = rating,
        pages = pages,
    )

    @Test
    fun sort_default_preservesOriginalOrder() {
        val g1 = createGallery(1L, "B", rating = 2f, pages = 50)
        val g2 = createGallery(2L, "A", rating = 5f, pages = 10)
        val config = DesktopSortConfig(field = DesktopSortField.Default)

        val result = config.sort(listOf(g1, g2))
        assertEquals(listOf(g1, g2), result)

        // 三角验证：空列表与单元素列表
        assertEquals(emptyList(), config.sort(emptyList()))
        assertEquals(listOf(g1), config.sort(listOf(g1)))
    }

    @Test
    fun sort_byRating_descendingAndAscending() {
        val g1 = createGallery(1L, rating = 2.0f)
        val g2 = createGallery(2L, rating = 4.5f)
        val g3 = createGallery(3L, rating = 3.0f)
        val list = listOf(g1, g2, g3)

        val desc = DesktopSortConfig(DesktopSortField.Rating, DesktopSortDirection.Descending).sort(list)
        assertEquals(listOf(g2, g3, g1), desc)

        val asc = DesktopSortConfig(DesktopSortField.Rating, DesktopSortDirection.Ascending).sort(list)
        assertEquals(listOf(g1, g3, g2), asc)
    }

    @Test
    fun sort_byPages_descendingAndAscending() {
        val g1 = createGallery(1L, pages = 100)
        val g2 = createGallery(2L, pages = 20)
        val g3 = createGallery(3L, pages = 300)
        val list = listOf(g1, g2, g3)

        val desc = DesktopSortConfig(DesktopSortField.Pages, DesktopSortDirection.Descending).sort(list)
        assertEquals(listOf(g3, g1, g2), desc)

        val asc = DesktopSortConfig(DesktopSortField.Pages, DesktopSortDirection.Ascending).sort(list)
        assertEquals(listOf(g2, g1, g3), asc)
    }

    @Test
    fun sort_byTitle_caseInsensitive() {
        val g1 = createGallery(1L, title = "Banana")
        val g2 = createGallery(2L, title = "apple")
        val g3 = createGallery(3L, title = "Cherry")
        val list = listOf(g1, g2, g3)

        val asc = DesktopSortConfig(DesktopSortField.Title, DesktopSortDirection.Ascending).sort(list)
        assertEquals(listOf(g2, g1, g3), asc)

        val desc = DesktopSortConfig(DesktopSortField.Title, DesktopSortDirection.Descending).sort(list)
        assertEquals(listOf(g3, g1, g2), desc)

        // 三角验证：null 标题与空白标题回退至 GID 字典序
        val g4 = createGallery(4L, title = null)
        val g5 = createGallery(5L, title = "   ")
        val fallbackSorted = DesktopSortConfig(DesktopSortField.Title, DesktopSortDirection.Ascending)
            .sort(listOf(g5, g4))
        assertEquals(listOf(g4, g5), fallbackSorted)
    }

    @Test
    fun cycleAndLabel_behaveAsExpected() {
        var cfg = DesktopSortConfig()
        assertEquals("Sort", cfg.label)

        cfg = cfg.cycle()
        assertEquals(DesktopSortConfig(DesktopSortField.Rating, DesktopSortDirection.Descending), cfg)
        assertEquals("★↓", cfg.label)

        cfg = cfg.cycle()
        assertEquals(DesktopSortConfig(DesktopSortField.Rating, DesktopSortDirection.Ascending), cfg)
        assertEquals("★↑", cfg.label)

        cfg = cfg.cycle()
        assertEquals(DesktopSortConfig(DesktopSortField.Pages, DesktopSortDirection.Descending), cfg)
        assertEquals("P↓", cfg.label)

        cfg = cfg.cycle()
        assertEquals(DesktopSortConfig(DesktopSortField.Pages, DesktopSortDirection.Ascending), cfg)
        assertEquals("P↑", cfg.label)

        cfg = cfg.cycle()
        assertEquals(DesktopSortConfig(DesktopSortField.Title, DesktopSortDirection.Descending), cfg)
        assertEquals("A-Z↓", cfg.label)

        cfg = cfg.cycle()
        assertEquals(DesktopSortConfig(DesktopSortField.Title, DesktopSortDirection.Ascending), cfg)
        assertEquals("A-Z↑", cfg.label)

        cfg = cfg.cycle()
        assertEquals(DesktopSortConfig(DesktopSortField.Default), cfg)
        assertEquals("Sort", cfg.label)
    }

    @Test
    fun nextAndToggle_cycleCorrectly() {
        assertEquals(DesktopSortField.Rating, DesktopSortField.Default.next())
        assertEquals(DesktopSortField.Pages, DesktopSortField.Rating.next())
        assertEquals(DesktopSortField.Title, DesktopSortField.Pages.next())
        assertEquals(DesktopSortField.Default, DesktopSortField.Title.next())

        assertEquals(DesktopSortDirection.Ascending, DesktopSortDirection.Descending.toggle())
        assertEquals(DesktopSortDirection.Descending, DesktopSortDirection.Ascending.toggle())
    }
}
