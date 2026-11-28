package com.hippo.ehviewer.client.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GalleryPageUrlParserTest {

    private fun assertResult(expected: GalleryPageUrlParser.Result?, actual: GalleryPageUrlParser.Result?) {
        if (expected == null) {
            assertNull(actual)
            return
        }
        assertEquals(expected.gid, actual?.gid)
        assertEquals(expected.pToken, actual?.pToken)
        assertEquals(expected.page, actual?.page)
    }

    @Test
    fun parse_strictUrl_returnsGalleryPage() {
        assertResult(
            GalleryPageUrlParser.Result(901103L, "91ea4b6d89", 11),
            GalleryPageUrlParser.parse("https://exhentai.org/s/91ea4b6d89/901103-12"),
        )
        assertResult(
            GalleryPageUrlParser.Result(901103L, "91ea4b6d89", 0),
            GalleryPageUrlParser.parse("https://e-hentai.org/s/91ea4b6d89/901103-1"),
        )
        assertResult(
            GalleryPageUrlParser.Result(901103L, "91ea4b6d89", 0),
            GalleryPageUrlParser.parse("https://e-hentai.org/lofi/s/91ea4b6d89/901103-1"),
        )
    }

    @Test
    fun parse_looseUrl_returnsGalleryPage() {
        assertResult(
            GalleryPageUrlParser.Result(901103L, "91ea4b6d89", 11),
            GalleryPageUrlParser.parse("91ea4b6d89/901103-12", strict = false),
        )
    }

    @Test
    fun parse_nonNumericOrZeroPage_isRejected() {
        assertNull(GalleryPageUrlParser.parse("https://exhentai.org/s/91ea4b6d89/901103-0"))
        assertNull(GalleryPageUrlParser.parse("https://exhentai.org/s/91ea4b6d89/901103-99999999999"))
    }

    @Test
    fun parse_overflowingGid_isRejected() {
        assertNull(GalleryPageUrlParser.parse("91ea4b6d89/99999999999999999999-1", strict = false))
    }

    @Test
    fun parse_foreignHostInStrictMode_returnsNull() {
        assertNull(GalleryPageUrlParser.parse("https://evil.example/s/91ea4b6d89/901103-12"))
    }
}
