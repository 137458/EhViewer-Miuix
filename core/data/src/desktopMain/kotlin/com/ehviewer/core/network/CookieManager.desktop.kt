package com.ehviewer.core.network

import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.http.parseClientCookiesHeader
import io.ktor.http.renderSetCookieHeader
import java.net.CookieManager as JavaCookieManager
import java.net.CookiePolicy
import java.net.URI

private class DesktopCookieManager : CookieManager {
    private val manager = JavaCookieManager().apply { setCookiePolicy(CookiePolicy.ACCEPT_ALL) }

    override fun getCookies(url: Url): Map<String, String>? {
        val headers = manager.get(URI(url.toString()), null) ?: return null
        val raw = headers["Set-Cookie"]?.joinToString("; ") ?: return null
        return parseClientCookiesHeader(raw)
    }

    override fun setCookie(url: Url, cookie: Cookie) {
        manager.put(URI(url.toString()), mapOf("Set-Cookie" to listOf(renderSetCookieHeader(cookie))))
    }

    override fun removeAllCookies() {
        manager.cookieStore.removeAll()
    }

    // java.net.CookieStore 为内存实现，无持久化批次可刷写
    override fun flush() = Unit
}

actual fun getCookieManager(): CookieManager = DesktopCookieManager()
