package com.ehviewer.core.network

import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import java.net.HttpCookie
import java.net.URI
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import okio.FileSystem
import okio.Path

// RFC 6265 桌面存储语义：每个用例独立 store 实例，避免全局单例串状态
class DesktopCookieStoreTest {
    private val created = mutableListOf<Path>()

    @AfterTest
    fun cleanup() {
        created.forEach { runCatching { FileSystem.SYSTEM.delete(it) } }
    }

    private fun newStore(): DesktopCookieStore {
        val path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "cookie_store_${System.nanoTime()}.dat"
        created += path
        return DesktopCookieStore { path }
    }

    @Test
    fun rejectsDomainAttributeNotCoveringRequestHost() {
        val store = newStore()
        store.add(
            URI("https://e-hentai.org/"),
            HttpCookie("k", "v").apply {
                domain = ".evil.example"
                path = "/"
            },
        )
        assertTrue(store.getCookies().isEmpty(), "Domain 属性不覆盖请求 Host 的 cookie 必须拒收")
    }

    @Test
    fun rejectsTopLevelSingleLabelDomainAttribute() {
        val store = newStore()
        store.add(
            URI("https://e-hentai.org/"),
            HttpCookie("k", "v").apply {
                domain = "com"
                path = "/"
            },
        )
        assertTrue(store.getCookies().isEmpty(), "顶层单标签 Domain（supercookie）必须拒收")
    }

    @Test
    fun acceptsParentDomainAttributeAndServesSubdomains() {
        val store = newStore()
        store.add(
            URI("https://forums.e-hentai.org/"),
            HttpCookie("k", "v").apply {
                domain = ".e-hentai.org"
                path = "/"
            },
        )
        assertEquals(1, store.get(URI("https://e-hentai.org/")).size)
        assertEquals(1, store.get(URI("https://forums.e-hentai.org/")).size)
        assertTrue(store.get(URI("https://evil.example/")).isEmpty())
    }

    @Test
    fun hostOnlyCookieIsNotServedToSubdomain() {
        val store = newStore()
        store.add(URI("https://e-hentai.org/"), HttpCookie("k", "v").apply { path = "/" })
        assertEquals(1, store.get(URI("https://e-hentai.org/")).size)
        assertTrue(store.get(URI("https://forums.e-hentai.org/")).isEmpty(), "host-only cookie 不得下沉到子域")
    }

    @Test
    fun secureCookieIsNotServedOverPlainHttp() {
        val store = newStore()
        store.add(
            URI("https://e-hentai.org/"),
            HttpCookie("k", "v").apply {
                path = "/"
                setSecure(true)
            },
        )
        assertTrue(store.get(URI("http://e-hentai.org/")).isEmpty(), "Secure cookie 不得随明文 http 请求发出")
        assertEquals(1, store.get(URI("https://e-hentai.org/")).size)
    }

    @Test
    fun zeroMaxAgeExpiresExistingCookie() {
        val store = newStore()
        val uri = URI("https://e-hentai.org/")
        store.add(uri, HttpCookie("k", "v").apply { path = "/" })
        assertEquals(1, store.get(uri).size)
        store.add(
            uri,
            HttpCookie("k", "deleted").apply {
                path = "/"
                maxAge = 0
            },
        )
        assertTrue(store.get(uri).isEmpty(), "maxAge=0 必须删除同名 cookie")
    }

    @Test
    fun expiredAbsoluteExpiryPurgesCookie() {
        val manager = DesktopCookieManager(newStore())
        val url = Url("https://e-hentai.org/")
        manager.setCookie(url, Cookie(name = "k", value = "v", expires = GMTDate(0L), path = "/"))
        assertNull(manager.getCookies(url), "Expires 已过去的 cookie 不得返回")
    }

    @Test
    fun managerKeepsHostOnlyCookiesHostOnly() {
        val manager = DesktopCookieManager(newStore())
        manager.setCookie(Url("https://e-hentai.org/"), Cookie(name = "k", value = "v", path = "/"))
        assertEquals("v", manager.getCookies(Url("https://e-hentai.org/"))?.get("k"))
        assertNull(manager.getCookies(Url("https://forums.e-hentai.org/")), "未带 Domain 属性的 cookie 是 host-only")
    }

    @Test
    fun sameNameDomainPathFromDifferentRequestPathsCollapsesToOneEntry() {
        val store = newStore()
        store.add(URI("https://e-hentai.org/foo"), HttpCookie("k", "v1").apply { path = "/" })
        store.add(URI("https://e-hentai.org/bar"), HttpCookie("k", "v2").apply { path = "/" })
        assertEquals(1, store.getCookies().size, "同一 (domain, path, name) 只能存一条")
        assertEquals("v2", store.getCookies().single().value)
    }

    @Test
    fun storedCookiesSurviveNewStoreInstance() {
        val file = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "cookie_store_roundtrip_${System.nanoTime()}.dat"
        created += file
        val writer = DesktopCookieStore { file }
        writer.add(URI("https://e-hentai.org/"), HttpCookie("k", "v").apply { path = "/" })

        val reader = DesktopCookieStore { file }
        assertEquals(listOf("k" to "v"), reader.getCookies().map { it.name to it.value })
    }

    @Test
    fun removeAllClearsPersistedState() {
        val file = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "cookie_store_clear_${System.nanoTime()}.dat"
        created += file
        val store = DesktopCookieStore { file }
        store.add(URI("https://e-hentai.org/"), HttpCookie("k", "v").apply { path = "/" })
        store.removeAll()

        assertTrue(DesktopCookieStore { file }.getCookies().isEmpty())
    }
}
