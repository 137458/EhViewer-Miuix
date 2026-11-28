package com.hippo.ehviewer.client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EhUrlSafetyTest {

    private val base = "https://e-hentai.org/g/123/abc/"

    @Test
    fun redactUrlForLog_masksCredentialParameters() {
        assertEquals(
            "https://e-hentai.org/g/123/abc/?token=***&page=1",
            redactUrlForLog("https://e-hentai.org/g/123/abc/?token=secret&page=1"),
        )
        assertEquals("https://e-hentai.org/x?hath=***", redactUrlForLog("https://e-hentai.org/x?hath=deadbeef"))
        assertEquals("https://e-hentai.org/x?TOKEN=***", redactUrlForLog("https://e-hentai.org/x?TOKEN=deadbeef"))
        assertEquals("https://e-hentai.org/x?key=***&secret=***", redactUrlForLog("https://e-hentai.org/x?key=1&secret=2"))
    }

    @Test
    fun redactUrlForLog_keepsHarmlessPartsIntact() {
        assertEquals("https://e-hentai.org/x", redactUrlForLog("https://e-hentai.org/x"))
        assertEquals("https://e-hentai.org/x?page=2&next=3", redactUrlForLog("https://e-hentai.org/x?page=2&next=3"))
        assertEquals("https://e-hentai.org/x?flag", redactUrlForLog("https://e-hentai.org/x?flag"))
        assertEquals("https://e-hentai.org/x?token=***#frag", redactUrlForLog("https://e-hentai.org/x?token=abc#frag"))
    }

    @Test
    fun isEhUrl_acceptsOnlyHentaiHosts() {
        assertTrue(isEhUrl("https://e-hentai.org/g/1/"))
        assertTrue(isEhUrl("http://exhentai.org/g/1/"))
        assertTrue(isEhUrl("https://sub.e-hentai.org/g/1/"))
        assertFalse(isEhUrl("https://notexhentai.org/g/1/"))
        assertFalse(isEhUrl("ftp://e-hentai.org/g/1/"))
        assertFalse(isEhUrl("https://evil.example/?next=https://e-hentai.org/"))
        assertFalse(isEhUrl("not a url"))
    }

    @Test
    fun resolveRedirectUrl_resolvesRelativeLocation() {
        assertEquals("https://e-hentai.org/g/124/def/", resolveRedirectUrl(base, "/g/124/def/"))
        assertEquals("https://e-hentai.org/g/123/abc/next", resolveRedirectUrl(base, "next"))
        assertEquals("https://exhentai.org/g/124/def/", resolveRedirectUrl(base, "https://exhentai.org/g/124/def/"))
    }

    @Test
    fun resolveRedirectUrl_rejectsForeignAndMissingLocation() {
        assertNull(resolveRedirectUrl(base, "https://evil.example/g/124/"))
        assertNull(resolveRedirectUrl(base, null))
        assertNull(resolveRedirectUrl(base, "   "))
        assertNull(resolveRedirectUrl(base, "javascript:alert(1)"))
    }
}
