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
    fun openUrlBrowseFailureTriggersWindowsFallback() {
        // "Failed to launch browser"（AWT 关联失败）→ rundll32 shell 回退仍能打开
        var launched: List<String>? = null
        val result = DesktopBrowser.openUrl(
            "https://e-hentai.org/g/123/abc/",
            launcher = { launched = it },
            osName = "Windows 11",
        ) {
            throw IOException("Failed to launch browser")
        }
        assertTrue(result)
        assertEquals(
            listOf("rundll32", "url.dll,FileProtocolHandler", "https://e-hentai.org/g/123/abc/"),
            launched,
        )
    }

    @Test
    fun openUrlFallsBackToShellWhenBrowseFails() {
        // AWT browse 失败（如 UWP 默认浏览器关联失败）→ Windows shell 回退仍能打开
        var fallbackCmd: List<String>? = null
        val result = DesktopBrowser.openUrl(
            "https://e-hentai.org/g/1/x/",
            opener = { throw IOException("Failed to launch browser") },
            launcher = { cmd -> fallbackCmd = cmd },
            osName = "Windows 11",
        )
        assertTrue(result)
        assertEquals(listOf("rundll32", "url.dll,FileProtocolHandler", "https://e-hentai.org/g/1/x/"), fallbackCmd)
    }

    @Test
    fun fallbackSkippedOnNonWindows() {
        var fallbackCmd: List<String>? = null
        val result = DesktopBrowser.openUrl(
            "https://e-hentai.org/g/1/x/",
            opener = { throw IOException("no desktop") },
            launcher = { cmd -> fallbackCmd = cmd },
            osName = "Linux",
        )
        assertFalse(result)
        assertEquals(null, fallbackCmd)
    }

    @Test
    fun fallbackCommandWindowsOnly() {
        assertEquals(
            listOf("rundll32", "url.dll,FileProtocolHandler", "https://a.example/"),
            DesktopBrowser.fallbackCommand("https://a.example/", osName = "Windows 10"),
        )
        assertEquals(null, DesktopBrowser.fallbackCommand("https://a.example/", osName = "Mac OS X"))
    }

    @Test
    fun openUrlSuccessSkipsFallback() {
        var fallbackCmd: List<String>? = null
        val result = DesktopBrowser.openUrl(
            "https://e-hentai.org/",
            opener = { },
            launcher = { cmd -> fallbackCmd = cmd },
            osName = "Windows 11",
        )
        assertTrue(result)
        assertEquals(null, fallbackCmd)
    }
}
