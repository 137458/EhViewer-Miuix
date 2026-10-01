package com.ehviewer.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
