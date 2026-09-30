package com.ehviewer.core.network

import com.ehviewer.core.desktop.testing.clearIsolatedDataDir
import com.ehviewer.core.desktop.testing.newIsolatedDataDir
import io.ktor.http.Cookie
import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.io.readByteArray
import okio.FileSystem

// 桌面侧 CookieManager actual 的运行时验证：写读回环 + 同步落盘 + 域/路径匹配语义
class CookieManagerDesktopTest {
    @Test
    fun setCookieRoundtripAndPersistToDisk() {
        val dataDir = newIsolatedDataDir()
        try {
            val manager = getCookieManager()
            val url = Url("https://exhentai.org")
            val name = "smoke_cookie_${System.currentTimeMillis()}"
            manager.setCookie(url, Cookie(name = name, value = "42", domain = ".exhentai.org"))

            assertEquals("42", manager.getCookies(url)?.get(name))
            manager.flush()

            // PersistentCookieStore 在变更时同步写盘；落盘内容为序列化 CookieRecord，
            // ASCII 字段名可直接在字节流中检索（DesktopDirs 在数据根下再分 EhViewer/files/）
            val file = dataDir / "EhViewer" / "files" / "cookies.dat"
            val fs = FileSystem.SYSTEM
            assertTrue(fs.exists(file), "persisted store should exist at $file")
            val bytes = fs.read(file) { readByteArray() }
            assertTrue(name in String(bytes, Charsets.ISO_8859_1), "persisted store should contain $name")
        } finally {
            clearIsolatedDataDir()
        }
    }

    @Test
    fun domainCookieMatchesSubdomainRequests() {
        val dataDir = newIsolatedDataDir()
        try {
            val manager = getCookieManager()
            val name = "domain_cookie_${System.currentTimeMillis()}"
            manager.setCookie(
                url = Url("https://e-hentai.org"),
                cookie = Cookie(name = name, value = "42", domain = ".e-hentai.org", path = "/"),
            )

            // 与 java.net.CookieManager 一致：.e-hentai.org 域 Cookie 对子域请求可见
            assertEquals("42", manager.getCookies(Url("https://forums.e-hentai.org"))?.get(name))
        } finally {
            clearIsolatedDataDir()
        }
    }

    @Test
    fun pathScopedCookieDoesNotLeakToSiblingPaths() {
        val dataDir = newIsolatedDataDir()
        try {
            val manager = getCookieManager()
            val name = "path_cookie_${System.currentTimeMillis()}"
            manager.setCookie(
                url = Url("https://e-hentai.org/foo/bar"),
                cookie = Cookie(name = name, value = "42", path = "/foo"),
            )

            assertEquals("42", manager.getCookies(Url("https://e-hentai.org/foo/baz"))?.get(name))
            assertEquals(null, manager.getCookies(Url("https://e-hentai.org/other"))?.get(name))
        } finally {
            clearIsolatedDataDir()
        }
    }
}
