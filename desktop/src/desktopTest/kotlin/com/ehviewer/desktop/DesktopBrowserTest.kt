package com.ehviewer.desktop

import java.io.IOException
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopBrowserTest {
    // 会走 Windows shell 兜底的畸形 URL：空白/引号/尖括号/控制字符必须在校验阶段被拒
    private val malformedUrls = listOf(
        "https://e-hentai.org/a b",
        "https://e-hentai.org/a\tb",
        "https://e-hentai.org/a\nb",
        "https://e-hentai.org/\"quoted\"",
        "https://e-hentai.org/<script>",
        "https://e-hentai.org/\u0000",
        "  ",
        "",
    )

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
        assertFalse(DesktopBrowser.isValidHttpUrl("ftp://example.com"))
        assertFalse(DesktopBrowser.isValidHttpUrl("javascript:alert(1)"))
        assertFalse(DesktopBrowser.isValidHttpUrl("http://"))
        assertFalse(DesktopBrowser.isValidHttpUrl("https://"))
        assertFalse(DesktopBrowser.isValidHttpUrl("file:///C:/path"))
    }

    @Test
    fun malformedUrlsRejectedBeforeAnyLaunch() {
        malformedUrls.forEach { url ->
            assertFalse(DesktopBrowser.isValidHttpUrl(url), "应拒绝: $url")
            var opened = false
            var launched: List<String>? = null
            val result = DesktopBrowser.openUrl(
                url,
                launcher = { cmd -> launched = cmd },
                osName = "Windows 11",
            ) { opened = true }
            assertFalse(result, "畸形 URL 不应打开: $url")
            assertFalse(opened, "畸形 URL 不应交给 AWT: $url")
            assertNull(launched, "畸形 URL 不应进入 shell 兜底: $url")
        }
    }

    @Test
    fun openUrlSuccessCallsOpenerAndReturnsTrue() {
        var openedUri: URI? = null
        var launched: List<String>? = null
        val result = DesktopBrowser.openUrl(
            url = "https://e-hentai.org/g/123/abc/",
            launcher = { cmd -> launched = cmd },
            osName = "Windows 11",
        ) { uri ->
            openedUri = uri
        }
        assertTrue(result)
        assertEquals("https://e-hentai.org/g/123/abc/", openedUri?.toString())
        // 打开成功不得再走 shell 兜底
        assertNull(launched)
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
    fun awtFailureFallsBackToWindowsShell() {
        // AWT 浏览器关联失败（UWP 默认浏览器等场景）→ rundll32 url.dll 协议处理器兜底
        var launched: List<String>? = null
        val result = DesktopBrowser.openUrl(
            url = "https://e-hentai.org/g/123/abc/",
            launcher = { cmd -> launched = cmd },
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
    fun fallbackSkippedOnNonWindows() {
        var fallbackCmd: List<String>? = null
        val result = DesktopBrowser.openUrl(
            url = "https://e-hentai.org/g/1/x/",
            launcher = { cmd -> fallbackCmd = cmd },
            osName = "Linux",
        ) {
            throw IOException("no desktop")
        }
        assertFalse(result)
        assertNull(fallbackCmd)
    }

    @Test
    fun fallbackCommandWindowsOnly() {
        assertEquals(
            listOf("rundll32", "url.dll,FileProtocolHandler", "https://a.example/"),
            DesktopBrowser.fallbackCommand("https://a.example/", osName = "Windows 10"),
        )
        assertNull(DesktopBrowser.fallbackCommand("https://a.example/", osName = "Mac OS X"))
    }

    @Test
    fun fallbackCommandRejectsMalformedUrl() {
        malformedUrls.forEach { url ->
            assertNull(DesktopBrowser.fallbackCommand(url, osName = "Windows 11"), "应拒绝: $url")
        }
    }
}
