package com.ehviewer.desktop

import com.ehviewer.core.database.model.GalleryEntity
import com.ehviewer.core.model.BaseGalleryInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DesktopFavoritesStateTest {
    private fun createGallery(
        gid: Long,
        title: String = "Gallery $gid",
        token: String = "token$gid",
    ): BaseGalleryInfo = BaseGalleryInfo(
        gid = gid,
        token = token,
        title = title,
        titleJpn = "Jpn $title",
        thumbKey = "thumb/$gid.jpg",
        category = 2,
        posted = "2026-10-01",
        uploader = "Alice",
        rating = 4.5f,
        simpleTags = listOf("tag1", "tag2"),
        pages = 100,
        simpleLanguage = "Japanese",
    )

    @Test
    fun isFavorite_checksPresenceCorrectly() {
        val faves = setOf(101L, 102L)
        assertTrue(DesktopFavoritesState.isFavorite(faves, 101L))
        assertFalse(DesktopFavoritesState.isFavorite(faves, 103L))
        // 三角验证：空集合与额外边界
        assertFalse(DesktopFavoritesState.isFavorite(emptySet(), 101L))
        assertFalse(DesktopFavoritesState.isFavorite(faves, 0L))
        assertFalse(DesktopFavoritesState.isFavorite(faves, -1L))
    }

    @Test
    fun toggleFavoriteGid_addsWhenMissing_removesWhenPresent() {
        val initial = setOf(101L, 102L)
        val added = DesktopFavoritesState.toggleFavoriteGid(initial, 103L)
        assertEquals(setOf(101L, 102L, 103L), added)

        val removed = DesktopFavoritesState.toggleFavoriteGid(added, 101L)
        assertEquals(setOf(102L, 103L), removed)

        // 三角验证：针对空集合添加，以及对单元素移除至空
        val fromEmpty = DesktopFavoritesState.toggleFavoriteGid(emptySet(), 999L)
        assertEquals(setOf(999L), fromEmpty)
        val toEmpty = DesktopFavoritesState.toggleFavoriteGid(fromEmpty, 999L)
        assertEquals(emptySet(), toEmpty)
    }

    @Test
    fun filterFavorites_retainsOnlyFavoritedGalleriesInOrder() {
        val g1 = createGallery(101L)
        val g2 = createGallery(102L)
        val g3 = createGallery(103L)
        val faves = setOf(101L, 103L)

        val filtered = DesktopFavoritesState.filterFavorites(listOf(g1, g2, g3), faves)
        assertEquals(listOf(g1, g3), filtered)

        // 三角验证：空列表输入、空收藏集合输入、重复项去重
        assertEquals(emptyList(), DesktopFavoritesState.filterFavorites(emptyList(), faves))
        assertEquals(emptyList(), DesktopFavoritesState.filterFavorites(listOf(g1, g2), emptySet()))
        val duplicated = listOf(g1, g1, g3, g2)
        assertEquals(listOf(g1, g3), DesktopFavoritesState.filterFavorites(duplicated, faves))
    }

    @Test
    fun updateSelectionAfterRemoveFavorite_clearsSelectionOnlyWhenInFavoritesTabAndSameGid() {
        val g1 = createGallery(101L)
        val g2 = createGallery(102L)

        // Case 1: In favorites tab and removed item is selected -> clear selection
        assertNull(
            DesktopFavoritesState.updateSelectionAfterRemoveFavorite(
                selected = g1,
                removedGid = 101L,
                isInFavoritesTab = true,
            ),
        )

        // Case 2: In favorites tab but another item is selected -> preserve selection
        assertSame(
            g2,
            DesktopFavoritesState.updateSelectionAfterRemoveFavorite(
                selected = g2,
                removedGid = 101L,
                isInFavoritesTab = true,
            ),
        )

        // Case 3: In another tab (e.g. History) -> preserve selection even if un-favorited
        assertSame(
            g1,
            DesktopFavoritesState.updateSelectionAfterRemoveFavorite(
                selected = g1,
                removedGid = 101L,
                isInFavoritesTab = false,
            ),
        )

        // 三角验证：原本 selected 为 null 时无论何种场景均安全返回 null
        assertNull(
            DesktopFavoritesState.updateSelectionAfterRemoveFavorite(
                selected = null,
                removedGid = 101L,
                isInFavoritesTab = true,
            ),
        )
        assertNull(
            DesktopFavoritesState.updateSelectionAfterRemoveFavorite(
                selected = null,
                removedGid = 101L,
                isInFavoritesTab = false,
            ),
        )
    }

    @Test
    fun toGalleryEntity_convertsBaseGalleryInfoPreservingFields() {
        val base = createGallery(200L, title = "Original Title", token = "tok200")
        val entity = DesktopFavoritesState.toGalleryEntity(base)

        assertEquals(200L, entity.gid)
        assertEquals("tok200", entity.token)
        assertEquals("Original Title", entity.title)
        assertEquals("Jpn Original Title", entity.titleJpn)
        assertEquals("thumb/200.jpg", entity.thumbKey)
        assertEquals(2, entity.category)
        assertEquals("2026-10-01", entity.posted)
        assertEquals("Alice", entity.uploader)
        assertEquals(4.5f, entity.rating)
        assertEquals(listOf("tag1", "tag2"), entity.simpleTags)
        assertEquals(100, entity.pages)
        assertEquals("Japanese", entity.simpleLanguage)

        // 三角验证：若入参本身已经是 GalleryEntity 则直接返回同一实例
        assertSame(entity, DesktopFavoritesState.toGalleryEntity(entity))
    }
}
