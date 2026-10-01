package com.ehviewer.desktop

import com.ehviewer.core.model.BaseGalleryInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GalleryFilterTest {

    private val sampleItems = listOf(
        BaseGalleryInfo(
            gid = 10001L,
            title = "Touhou Project Anthology",
            titleJpn = "東方Projectまとめ",
            uploader = "Alice",
            simpleTags = listOf("touhou", "parody"),
        ),
        BaseGalleryInfo(
            gid = 20002L,
            title = "Original Artbook Vol.1",
            titleJpn = "オリジナル画集",
            uploader = "Bob",
            simpleTags = listOf("original", "full color"),
        ),
        BaseGalleryInfo(
            gid = 30003L,
            title = "Fate Grand Order Doujin",
            titleJpn = "Fate/Grand Order 同人誌",
            uploader = "Charlie",
            simpleTags = listOf("fate", "female:saber"),
        ),
    )

    @Test
    fun filterGalleriesBlankQueryReturnsOriginalList() {
        val resultEmpty = GalleryFilter.filterGalleries(sampleItems, "")
        assertEquals(3, resultEmpty.size)

        val resultSpaces = GalleryFilter.filterGalleries(sampleItems, "   ")
        assertEquals(3, resultSpaces.size)
    }

    @Test
    fun filterGalleriesMatchesTitleCaseInsensitive() {
        val result = GalleryFilter.filterGalleries(sampleItems, "touhou")
        assertEquals(1, result.size)
        assertEquals(10001L, result.first().gid)

        val resultUpper = GalleryFilter.filterGalleries(sampleItems, "ORIGINAL")
        assertEquals(1, resultUpper.size)
        assertEquals(20002L, resultUpper.first().gid)
    }

    @Test
    fun filterGalleriesMatchesJapaneseTitle() {
        val result = GalleryFilter.filterGalleries(sampleItems, "画集")
        assertEquals(1, result.size)
        assertEquals(20002L, result.first().gid)
    }

    @Test
    fun filterGalleriesMatchesUploader() {
        val result = GalleryFilter.filterGalleries(sampleItems, "charlie")
        assertEquals(1, result.size)
        assertEquals(30003L, result.first().gid)
    }

    @Test
    fun filterGalleriesMatchesGid() {
        val result = GalleryFilter.filterGalleries(sampleItems, "20002")
        assertEquals(1, result.size)
        assertEquals(20002L, result.first().gid)
    }

    @Test
    fun filterGalleriesMatchesTags() {
        val result = GalleryFilter.filterGalleries(sampleItems, "saber")
        assertEquals(1, result.size)
        assertEquals(30003L, result.first().gid)
    }

    @Test
    fun filterGalleriesNonMatchingReturnsEmpty() {
        val result = GalleryFilter.filterGalleries(sampleItems, "nonexistent_query_404")
        assertTrue(result.isEmpty())
    }
}
