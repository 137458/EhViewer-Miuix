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
            category = com.ehviewer.core.database.client.CATEGORY_DOUJINSHI,
            simpleTags = listOf("touhou", "parody"),
        ),
        BaseGalleryInfo(
            gid = 20002L,
            title = "Original Artbook Vol.1",
            titleJpn = "オリジナル画集",
            uploader = "Bob",
            category = com.ehviewer.core.database.client.CATEGORY_ARTIST_CG,
            simpleTags = listOf("original", "full color"),
        ),
        BaseGalleryInfo(
            gid = 30003L,
            title = "Fate Grand Order Doujin",
            titleJpn = "Fate/Grand Order 同人誌",
            uploader = "Charlie",
            category = com.ehviewer.core.database.client.CATEGORY_COSPLAY,
            simpleTags = listOf("fate", "female:saber"),
        ),
    )

    @Test
    fun filterGalleriesMatchesCategoryNameOrDisplayName() {
        val resultDoujin = GalleryFilter.filterGalleries(sampleItems, "doujinshi")
        assertEquals(1, resultDoujin.size)
        assertEquals(10001L, resultDoujin.first().gid)

        val resultArtist = GalleryFilter.filterGalleries(sampleItems, "Artist CG")
        assertEquals(1, resultArtist.size)
        assertEquals(20002L, resultArtist.first().gid)

        val resultCosplay = GalleryFilter.filterGalleries(sampleItems, "cosplay")
        assertEquals(1, resultCosplay.size)
        assertEquals(30003L, resultCosplay.first().gid)
    }

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
    fun filterGalleriesMatchesPastedEhentaiUrl() {
        val result = GalleryFilter.filterGalleries(sampleItems, "https://e-hentai.org/g/20002/abcdef1234/")
        assertEquals(1, result.size)
        assertEquals(20002L, result.first().gid)
    }

    @Test
    fun filterGalleriesMatchesPastedExhentaiUrl() {
        val result = GalleryFilter.filterGalleries(sampleItems, "https://exhentai.org/g/30003/1234567890/")
        assertEquals(1, result.size)
        assertEquals(30003L, result.first().gid)
    }

    @Test
    fun filterGalleriesNonMatchingReturnsEmpty() {
        val result = GalleryFilter.filterGalleries(sampleItems, "nonexistent_query_404")
        assertTrue(result.isEmpty())
    }

    @Test
    fun filterGalleriesDeduplicatesByGidKeepingFirstOccurrence() {
        // 列表以 gid 作为 Compose 条目 key，重复 gid 会触发 "Key was duplicated" 崩溃，须在源头去重
        val duplicated = listOf(
            sampleItems[0],
            sampleItems[1],
            BaseGalleryInfo(gid = 10001L, title = "Later Duplicate Of First"),
            sampleItems[2],
        )

        // 空查询路径（原样返回分支）同样必须去重
        val blankResult = GalleryFilter.filterGalleries(duplicated, "")
        assertEquals(listOf(10001L, 20002L, 30003L), blankResult.map { it.gid })
        assertEquals("Touhou Project Anthology", blankResult.first().title)

        // 关键字过滤路径去重
        val queryResult = GalleryFilter.filterGalleries(duplicated, "touhou")
        assertEquals(listOf(10001L), queryResult.map { it.gid })
    }
}
