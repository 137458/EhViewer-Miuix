package com.ehviewer.core.network

import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.http.parseClientCookiesHeader
import io.ktor.http.renderSetCookieHeader

class AndroidCookieManager : CookieManager {
    private val manager = android.webkit.CookieManager.getInstance()

    override fun getCookies(url: Url): Map<String, String>? {
        val raw = manager.getCookie(url.toString()) ?: return null
        return runCatching {
            parseClientCookiesHeader(raw)
        }.getOrElse {
            val result = mutableMapOf<String, String>()
            for (segment in raw.split(';')) {
                val parts = segment.split('=', limit = 2)
                if (parts.size == 2) {
                    val k = parts[0].trim()
                    val v = parts[1].trim()
                    if (k.isNotEmpty()) {
                        result[k] = v
                    }
                }
            }
            result
        }
    }
    override fun setCookie(url: Url, cookie: Cookie) = manager.setCookie(url.toString(), renderSetCookieHeader(cookie))
    override fun removeAllCookies() = manager.removeAllCookies(null)
    override fun flush() = manager.flush()
}

actual fun getCookieManager(): CookieManager = AndroidCookieManager()
