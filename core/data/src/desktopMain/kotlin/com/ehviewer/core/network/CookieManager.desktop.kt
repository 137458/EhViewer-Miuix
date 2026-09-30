package com.ehviewer.core.network

import com.ehviewer.core.DesktopDirs
import io.ktor.http.Cookie
import io.ktor.http.Url
import java.net.CookieStore
import java.net.HttpCookie
import java.net.URI

// 内存 cookie 在桌面重启即丢登录态；getCookieManager() 每次调用返回新 CookieManager，
// store 必须全局单例，否则多实例各自持内存态会互相覆盖
private object PersistentCookieStore : CookieStore {
    private val entries = LinkedHashMap<URI, MutableList<HttpCookie>>()

    // 每次按当前数据根构造（无状态轻对象），保证测试重定向 ehviewer.data.dir 即时生效
    private val persistence: CookiePersistence
        get() = CookiePersistence(DesktopDirs.filesDir / "cookies.dat")

    init {
        load()
    }

    override fun add(uri: URI, cookie: HttpCookie) {
        synchronized(this) {
            entryFor(uri).removeAll { it.name == cookie.name && it.path == cookie.path }
            entryFor(uri).add(cookie)
            save()
        }
    }

    override fun get(uri: URI): List<HttpCookie> = synchronized(this) {
        val host = uri.host?.lowercase()
        if (host.isNullOrEmpty()) {
            return@synchronized emptyList()
        }
        val path = uri.path?.ifEmpty { "/" } ?: "/"
        entries.entries
            .flatMap { (storedUri, cookies) -> cookies.map { storedUri to it } }
            .filter { (_, cookie) -> !cookie.hasExpired() }
            .filter { (storedUri, cookie) ->
                val domain = (cookie.domain ?: storedUri.host)?.lowercase()
                domain != null && HttpCookie.domainMatches(domain, host) && pathMatches(path, cookie.path)
            }
            .map { (_, cookie) -> cookie }
    }

    override fun getCookies(): List<HttpCookie> = synchronized(this) {
        entries.values.flatten().filter { !it.hasExpired() }
    }

    override fun getURIs(): List<URI> = synchronized(this) { entries.keys.toList() }

    override fun remove(uri: URI, cookie: HttpCookie): Boolean = synchronized(this) {
        val removed = entryFor(uri).removeAll { it.name == cookie.name && it.path == cookie.path }
        if (removed) save()
        removed
    }

    override fun removeAll(): Boolean = synchronized(this) {
        val hadAny = entries.isNotEmpty()
        entries.clear()
        save()
        hadAny
    }

    private fun entryFor(uri: URI): MutableList<HttpCookie> = entries.getOrPut(uri) { mutableListOf() }

    // RFC 6265 路径匹配的简化实现：请求路径须落在 Cookie 路径子树内
    private fun pathMatches(requestPath: String, cookiePath: String?): Boolean {
        val path = cookiePath?.ifEmpty { "/" } ?: "/"
        if (path == "/" || requestPath == path) return true
        return requestPath.startsWith(path) && (path.endsWith("/") || requestPath[path.length] == '/')
    }

    private fun save() {
        persistence.save(entries.toMap())
    }

    private fun load() {
        val restored = persistence.load()
        synchronized(this) {
            entries.clear()
            restored.forEach { (uri, cookies) ->
                entries.getOrPut(uri) { mutableListOf() }.addAll(cookies)
            }
        }
    }

    private fun synchronizedEntries(): Map<URI, List<HttpCookie>> = synchronized(this) {
        entries.toMap()
    }
}

private class DesktopCookieManager : CookieManager {
    override fun getCookies(url: Url): Map<String, String>? {
        val cookies = PersistentCookieStore.get(URI(url.toString()))
        if (cookies.isEmpty()) return null
        return cookies.associate { it.name to it.value }
    }

    override fun setCookie(url: Url, cookie: Cookie) {
        val uri = URI(url.toString())
        val httpCookie = HttpCookie(cookie.name, cookie.value).apply {
            domain = cookie.domain ?: uri.host
            path = cookie.path?.ifEmpty { "/" } ?: "/"
            setSecure(cookie.secure)
        }
        PersistentCookieStore.add(uri, httpCookie)
    }

    override fun removeAllCookies() {
        PersistentCookieStore.removeAll()
    }

    override fun flush() {
        // PersistentCookieStore 在每次变更时同步写盘，无积压批次可刷写
    }
}

actual fun getCookieManager(): CookieManager = DesktopCookieManager()
