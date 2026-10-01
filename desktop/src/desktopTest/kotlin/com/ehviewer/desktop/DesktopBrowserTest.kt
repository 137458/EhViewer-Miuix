package com.ehviewer.desktop

import java.io.IOException
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopBrowserTest {
    @Test
    fun validHttpUrlRecognized() {
        assertTrue(DesktopBrowser.isValidHttpUrl("https://e-hentai.org/g/12345/6789/"))
        assertTrue(DesktopBrowser.isValidHttpUrl("http://example.com/test"))
        assertTrue(DesktopBrowser.isValidHttpUrl("HTTPS://EXHENTAI.ORG/"))
        assertTrue(DesktopBrowser.isValidHttpUrl("http://127.0.0.1:8080/path?query=1#frag"))
        assertTrue(DesktopBrowser.isValidHttpUrl("  https://e-hentai.org/  "))
    }

    @Test
    fun invalidUrlRejected() {
        assertFalse(DesktopBrowser.isValidHttpUrl(""))
        assertFalse(DesktopBrowser.isValidHttpUrl("   "))
        assertFalse(DesktopBrowser.isValidHttpUrl("ftp://example.com"))
        assertFalse(DesktopBrowser.isValidHttpUrl("javascript:alert(1)"))
        assertFalse(DesktopBrowser.isValidHttpUrl("http://"))
        assertFalse(DesktopBrowser.isValidHttpUrl("https://"))
        assertFalse(DesktopBrowser.isValidHttpUrl("file:///C:/path"))
    }

    @Test
    fun openUrlSuccessCallsOpenerAndReturnsTrue() {
        var openedUri: URI? = null
        val result = DesktopBrowser.openUrl("https://e-hentai.org/g/123/abc/") { uri ->
            openedUri = uri
        }
        assertTrue(result)
        assertEquals("https://e-hentai.org/g/123/abc/", openedUri?.toString())
    }

    @Test
    fun openUrlInvalidUrlDoesNotCallOpener() {
        var called = false
        val result = DesktopBrowser.openUrl("not-a-valid-url") {
            called = true
        }
        assertFalse(result)
        assertFalse(called)
    }

    @Test
    fun openUrlExceptionReturnsFalse() {
        val result = DesktopBrowser.openUrl("https://e-hentai.org/g/123/abc/") {
            throw IOException("Failed to launch browser")
        }
        assertFalse(result)
    }
}
