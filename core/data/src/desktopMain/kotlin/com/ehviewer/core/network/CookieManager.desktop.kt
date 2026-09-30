package com.ehviewer.core.network

import io.ktor.http.Cookie
import io.ktor.http.Url
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.CookieStore
import java.net.HttpCookie
import java.net.URI
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

// Cookie 落盘目录与 DataStore 偏好同约定（%APPDATA%/EhViewer/files）
private val cookieStorePath: Path by lazy {
    val base = System.getenv("APPDATA")?.replace('\\', '/')
        ?: (System.getProperty("user.home") + "/.ehviewer")
    "$base/EhViewer/files/cookies.dat".toPath()
}

// 内存 cookie 在桌面重启即丢登录态；getCookieManager() 每次调用返回新 CookieManager，
// store 必须全局单例，否则多实例各自持内存态会互相覆盖
private object PersistentCookieStore : CookieStore {
    private val entries = LinkedHashMap<URI, MutableList<HttpCookie>>()
    private val fs = FileSystem.SYSTEM

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
        entries.entries
            .filter { (key, _) -> hostMatches(uri, key) }
            .flatMap { it.value }
            .filter { !it.hasExpired() }
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

    private fun hostMatches(requested: URI, stored: URI): Boolean = requested.host?.removePrefix(".") == stored.host?.removePrefix(".")

    private fun save() {
        runCatching {
            cookieStorePath.parent?.let { fs.createDirectories(it) }
            val records = synchronizedEntries().flatMap { (uri, cookies) ->
                cookies.map { c ->
                    CookieRecord(uri, c.getName(), c.getValue(), c.getDomain(), c.getPath(), c.secure)
                }
            }
            ObjectOutputStream(FileOutputStream(cookieStorePath.toString())).use { out ->
                out.writeObject(records)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun load() {
        if (!fs.exists(cookieStorePath)) return
        runCatching {
            ObjectInputStream(FileInputStream(cookieStorePath.toString())).use { input ->
                val records = input.readObject() as List<CookieRecord>
                synchronized(this) {
                    entries.clear()
                    records.forEach { r ->
                        entries.getOrPut(r.uri) { mutableListOf() }.add(r.toHttpCookie())
                    }
                }
            }
        }.onFailure {
            // 文件损坏时丢弃会话副本，等价于未登录状态，不阻塞启动
            entries.clear()
        }
    }

    private fun synchronizedEntries(): List<Pair<URI, List<HttpCookie>>> = synchronized(this) {
        entries.map { (uri, cookies) -> uri to cookies.toList() }
    }
}

// HttpCookie 不可序列化，落盘用自有 DTO；会话 cookie 重启后转为持久，保证桌面端登录态延续
private data class CookieRecord(
    val uri: URI,
    val name: String,
    val value: String,
    val domain: String?,
    val path: String?,
    val secure: Boolean,
) : java.io.Serializable {
    fun toHttpCookie() = HttpCookie(name, value).apply {
        domain = this@CookieRecord.domain
        path = this@CookieRecord.path?.ifEmpty { "/" } ?: "/"
        setSecure(secure)
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
