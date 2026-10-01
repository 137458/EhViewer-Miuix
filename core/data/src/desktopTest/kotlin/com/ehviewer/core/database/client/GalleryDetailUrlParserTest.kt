package com.ehviewer.core.database.client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GalleryDetailUrlParserTest {

    @Test
    fun parseStandardEhentaiUrl() {
        val url = "https://e-hentai.org/g/1234567/8b3c7e4a21/"
        val result = GalleryDetailUrlParser.parse(url)
        assertNotNull(result)
        assertEquals(1234567L, result.gid)
        assertEquals("8b3c7e4a21", result.token)
    }

    @Test
    fun parseExhentaiUrl() {
        val url = "https://exhentai.org/g/9876543/1234567890/"
        val result = GalleryDetailUrlParser.parse(url)
        assertNotNull(result)
        assertEquals(9876543L, result.gid)
        assertEquals("1234567890", result.token)
    }

    @Test
    fun parseLofiUrl() {
        val url = "https://e-hentai.org/lofi/g/555555/abcdef0123"
        val result = GalleryDetailUrlParser.parse(url)
        assertNotNull(result)
        assertEquals(555555L, result.gid)
        assertEquals("abcdef0123", result.token)
    }

    @Test
    fun parseMpvUrl() {
        val url = "https://e-hentai.org/mpv/777777/fe01234567/"
        val result = GalleryDetailUrlParser.parse(url)
        assertNotNull(result)
        assertEquals(777777L, result.gid)
        assertEquals("fe01234567", result.token)
    }

    @Test
    fun parseLenientMode() {
        val text = "Check out this gallery: 123456/abcdef0123 in comments"
        val strictResult = GalleryDetailUrlParser.parse(text, strict = true)
        assertNull(strictResult)

        val lenientResult = GalleryDetailUrlParser.parse(text, strict = false)
        assertNotNull(lenientResult)
        assertEquals(123456L, lenientResult.gid)
        assertEquals("abcdef0123", lenientResult.token)
    }

    @Test
    fun parseInvalidUrlReturnsNull() {
        assertNull(GalleryDetailUrlParser.parse(null))
        assertNull(GalleryDetailUrlParser.parse(""))
        assertNull(GalleryDetailUrlParser.parse("https://google.com"))
        assertNull(GalleryDetailUrlParser.parse("https://e-hentai.org/g/notanumber/8b3c7e4a21/"))
        assertNull(GalleryDetailUrlParser.parse("https://e-hentai.org/g/12345/short/"))
    }
}
