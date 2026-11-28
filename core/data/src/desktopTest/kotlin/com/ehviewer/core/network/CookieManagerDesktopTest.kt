package com.ehviewer.core.network

import com.ehviewer.core.desktop.testing.clearIsolatedDataDir
import com.ehviewer.core.desktop.testing.newIsolatedDataDir
import com.sun.jna.Platform
import io.ktor.http.Cookie
import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
            // 高熵值：密文里偶然撞上短字符串会误报前端断言
            val value = "test_pass_hash_9876543210abcdef"
            manager.setCookie(url, Cookie(name = name, value = value, domain = ".exhentai.org"))

            assertEquals(value, manager.getCookies(url)?.get(name))
            manager.flush()

            // store 在变更时同步写盘（DesktopDirs 在数据根下再分 EhViewer/files/）；
            // Windows 落盘为 DPAPI 密文：文件存在非空，且 cookie 键名/值不得以明文出现在字节流中；
            // 非 Windows 无 Crypt32，明文回环只验证存在与非空
            val file = dataDir / "EhViewer" / "files" / "cookies.dat"
            val fs = FileSystem.SYSTEM
            assertTrue(fs.exists(file), "persisted store should exist at $file")
            val bytes = fs.read(file) { readByteArray() }
            assertTrue(bytes.size > 0, "persisted store should not be empty")
            if (Platform.isWindows()) {
                val plaintext = String(bytes, Charsets.ISO_8859_1)
                assertTrue(name !in plaintext && value !in plaintext, "cookie must not be stored in plaintext")
            }
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

            // .e-hentai.org 域 Cookie 对子域请求可见
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
            assertNull(manager.getCookies(Url("https://e-hentai.org/other"))?.get(name))
        } finally {
            clearIsolatedDataDir()
        }
    }
}
