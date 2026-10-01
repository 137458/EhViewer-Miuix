package com.ehviewer.desktop

import com.ehviewer.core.database.client.thumbUrl
import com.ehviewer.core.model.BaseGalleryInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GalleryDisplayTest {
    @Test
    fun galleryWebUrlConstructsCorrectEhUrl() {
        assertEquals("https://e-hentai.org/g/123456/abcdef7890/", galleryWebUrl(123456L, "abcdef7890"))
        assertEquals("https://e-hentai.org/g/0//", galleryWebUrl(0L, ""))
    }

    @Test
    fun galleryDisplayTitleFallsBackToGidWhenBlank() {
        assertEquals("My Gallery", galleryDisplayTitle("My Gallery", 123L))
        assertEquals("123", galleryDisplayTitle("", 123L))
        assertEquals("123", galleryDisplayTitle(null, 123L))
        assertEquals("123", galleryDisplayTitle("   ", 123L))
    }

    @Test
    fun libraryTabFromNameParsesAndFallsBackToHistory() {
        assertEquals(LibraryTab.History, LibraryTab.fromName("History"))
        assertEquals(LibraryTab.Favorites, LibraryTab.fromName("Favorites"))
        assertEquals(LibraryTab.Online, LibraryTab.fromName("Online"))
        // 非法/空值安全回退（存储损坏或历史版本键）
        assertEquals(LibraryTab.History, LibraryTab.fromName(null))
        assertEquals(LibraryTab.History, LibraryTab.fromName(""))
        assertEquals(LibraryTab.History, LibraryTab.fromName("NoSuchTab"))
    }

    @Test
    fun galleryInfoThumbUrlExtensionProvidesHttpCoverUrl() {
        val gallery = BaseGalleryInfo(thumbKey = "t/12/34/56.jpg")
        assertEquals("https://ehgt.org/t/12/34/56.jpg", gallery.thumbUrl)
        val noThumb = BaseGalleryInfo(thumbKey = null)
        assertNull(noThumb.thumbUrl)
    }

    @Test
    fun formatGalleryTagsFormatsWithCommaSeparation() {
        assertEquals("touhou, parody, female:reimu", formatGalleryTags(listOf("touhou", "parody", "female:reimu")))
        assertEquals("single_tag", formatGalleryTags(listOf("single_tag")))
        assertEquals("", formatGalleryTags(emptyList()))
        assertEquals("", formatGalleryTags(null))
    }
}
