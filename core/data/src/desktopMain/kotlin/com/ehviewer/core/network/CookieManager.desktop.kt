package com.ehviewer.core.network

import com.ehviewer.core.DesktopDirs
import io.ktor.http.Cookie
import io.ktor.http.Url
import java.net.CookieStore
import java.net.HttpCookie
import java.net.URI
import okio.Path

// 落盘键：同一 cookie 无论由哪个请求 URI 写入都归并到一条记录
internal data class CookieKey(val domain: String, val path: String, val name: String)

// 内存 cookie 在桌面重启即丢登录态；getCookieManager() 每次调用返回新 CookieManager，
// store 必须全局单例，否则多实例各自持内存态会互相覆盖
internal class DesktopCookieStore(private val cookieFile: () -> Path) : CookieStore {
    private val entries = LinkedHashMap<CookieKey, CookieJsonRecord>()

    // 每次按当前数据根构造（无状态轻对象），保证测试重定向 ehviewer.data.dir 即时生效
    private val persistence: CookiePersistence
        get() = CookiePersistence(cookieFile())

    init {
        load()
    }

    override fun add(uri: URI, cookie: HttpCookie) {
        val host = normalizedHost(uri) ?: return
        val rawDomain = cookie.domain
        val hostOnly = rawDomain == null
        val domain = if (hostOnly) host else normalizeCookieDomain(rawDomain) ?: return
        // 域属性必须覆盖请求 host，且拒绝顶层单标签域（supercookie 可向任意站点广播）
        if (!hostOnly && !cookieDomainMatches(domain, host, hostOnly = false)) return
        if (!hostOnly && !domain.contains('.')) return
        val path = cookie.path?.let(::normalizeCookiePath) ?: defaultCookiePath(uri)
        val key = CookieKey(domain, path, cookie.name)
        val maxAgeSeconds = cookie.maxAge
        synchronized(this) {
            // maxAge=0 = 删除指令，不落盘新值
            if (maxAgeSeconds == 0L) {
                if (entries.remove(key) != null) save()
                return
            }
            val expiresAt = if (maxAgeSeconds > 0) System.currentTimeMillis() + maxAgeSeconds * 1000 else null
            entries[key] = CookieJsonRecord(
                name = cookie.name,
                value = cookie.value,
                domain = domain,
                path = path,
                secure = cookie.secure,
                hostOnly = hostOnly,
                expiresAtEpochMs = expiresAt,
            )
            save()
        }
    }

    override fun get(uri: URI): List<HttpCookie> = synchronized(this) {
        val host = normalizedHost(uri)
        if (host == null) {
            return@synchronized emptyList()
        }
        val now = System.currentTimeMillis()
        val requestPath = normalizeCookiePath(uri.path)
        // Secure cookie 只允许随 https 发往网络，明文通道下等同不存在
        val secureChannel = uri.scheme?.equals("https", ignoreCase = true) == true
        entries.values
            .filter { !it.isExpired(now) }
            .filter { cookieDomainMatches(it.domain, host, it.hostOnly) }
            .filter { cookiePathMatches(requestPath, it.path) }
            .filter { secureChannel || !it.secure }
            .sortedByDescending { it.path.length }
            .map { it.toHttpCookie(now) }
    }

    override fun getCookies(): List<HttpCookie> = synchronized(this) {
        val now = System.currentTimeMillis()
        entries.values.filter { !it.isExpired(now) }.map { it.toHttpCookie(now) }
    }

    override fun getURIs(): List<URI> = synchronized(this) {
        entries.values.map { URI("https", it.domain, it.path, null) }
    }

    override fun remove(uri: URI, cookie: HttpCookie): Boolean = synchronized(this) {
        val host = normalizedHost(uri) ?: return@synchronized false
        val domain = cookie.domain?.let(::normalizeCookieDomain) ?: host
        val path = cookie.path?.let(::normalizeCookiePath) ?: defaultCookiePath(uri)
        val removed = entries.remove(CookieKey(domain, path, cookie.name)) != null
        if (removed) save()
        removed
    }

    override fun removeAll(): Boolean = synchronized(this) {
        val hadAny = entries.isNotEmpty()
        entries.clear()
        save()
        hadAny
    }

    private fun normalizedHost(uri: URI): String? = uri.host?.lowercase()?.takeIf { it.isNotEmpty() }

    private fun defaultCookiePath(uri: URI): String {
        val requestPath = uri.path.orEmpty()
        if (!requestPath.startsWith("/")) return "/"
        val lastSlash = requestPath.lastIndexOf('/')
        return if (lastSlash <= 0) "/" else requestPath.substring(0, lastSlash)
    }

    private fun save() {
        persistence.save(entries.values.toList())
    }

    private fun load() {
        val restored = persistence.load()
        synchronized(this) {
            entries.clear()
            restored.forEach { entries[CookieKey(it.domain, it.path, it.name)] = it }
        }
    }
}

private val defaultCookieStore: DesktopCookieStore by lazy {
    DesktopCookieStore { DesktopDirs.filesDir / "cookies.dat" }
}

internal class DesktopCookieManager(private val store: CookieStore = defaultCookieStore) : CookieManager {
    override fun getCookies(url: Url): Map<String, String>? {
        val cookies = store.get(URI(url.toString()))
        if (cookies.isEmpty()) return null
        // get() 已按路径长度降序返回，同名 cookie 取路径更具体的一条（RFC 6265 §5.4）
        val result = LinkedHashMap<String, String>(cookies.size)
        cookies.forEach { result.putIfAbsent(it.name, it.value) }
        return result
    }

    override fun setCookie(url: Url, cookie: Cookie) {
        val uri = URI(url.toString())
        val maxAgeSeconds = cookie.maxAge
        val expiresAt = cookie.expires
        val httpCookie = HttpCookie(cookie.name, cookie.value).apply {
            // 域属性缺省时保持 null：由 store 记为 host-only，避免放宽到子域
            cookie.domain?.let { domain = it }
            cookie.path?.let { path = it }
            setSecure(cookie.secure)
            maxAge = when {
                maxAgeSeconds != null -> maxAgeSeconds.toLong()
                // 绝对过期换算成剩余秒数，store 再落成绝对时间戳
                expiresAt != null -> ((expiresAt.timestamp - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
                else -> -1L
            }
        }
        store.add(uri, httpCookie)
    }

    override fun removeAllCookies() {
        store.removeAll()
    }

    override fun flush() {
        // store 每次变更即同步写盘，无积压批次可刷写
    }
}

actual fun getCookieManager(): CookieManager = DesktopCookieManager()
