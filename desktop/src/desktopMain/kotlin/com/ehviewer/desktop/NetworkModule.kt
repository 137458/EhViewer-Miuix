package com.ehviewer.desktop

import com.ehviewer.core.network.EhCookieStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.userAgent
import java.net.InetSocketAddress
import java.net.ProxySelector

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
private const val CHROME_ACCEPT =
    "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8"

// 代理解析：设置项 > HTTPS_PROXY/HTTP_PROXY 环境变量 > 直连
private fun resolveProxySelector(): ProxySelector? {
    DesktopSettings.proxy.value?.trim()?.takeIf { it.isNotEmpty() }?.let { configured ->
        return parseHostPort(configured)?.let { ProxySelector.of(it) }
    }
    val fromEnv = System.getenv("HTTPS_PROXY") ?: System.getenv("HTTP_PROXY")
    if (!fromEnv.isNullOrBlank()) {
        parseHostPort(fromEnv.trim())?.let { return ProxySelector.of(it) }
    }
    return null
}

private fun parseHostPort(value: String): InetSocketAddress? {
    val match = Regex("^(https?://)?([^:/]+):(\\d+)$").matchEntire(value) ?: return null
    val (scheme, host, port) = match.destructured
    if (!scheme.isBlank() && !scheme.startsWith("http")) return null
    return runCatching { InetSocketAddress(host, port.toInt()) }.getOrNull()
}

// 桌面网络栈：与 Android 侧共享 EhCookieStore(ktor CookiesStorage)，
// 引擎用 okhttp（Cronet 仅 Android 可用），UA 固定桌面 Chrome。
// 代理在设置页修改后自动重建客户端。
private var clientCache: HttpClient? = null
private var clientProxyKey: String? = null
private val clientLock = Any()

fun acquireClient(): HttpClient = synchronized(clientLock) {
    val proxyKey = DesktopSettings.proxy.value
    val cached = clientCache
    if (cached == null || clientProxyKey != proxyKey) {
        cached?.close()
        clientCache = buildClient(proxySelector = resolveProxySelector())
        clientProxyKey = proxyKey
    }
    clientCache!!
}

private fun buildClient(proxySelector: ProxySelector?): HttpClient = HttpClient(OkHttp) {
    expectSuccess = false
    engine {
        config {
            if (proxySelector != null) {
                proxySelector(proxySelector)
            }
        }
    }
    install(HttpCookies) {
        storage = EhCookieStore
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        connectTimeoutMillis = 15_000
    }
    defaultRequest {
        header(HttpHeaders.Accept, CHROME_ACCEPT)
        header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9")
        userAgent(DESKTOP_USER_AGENT)
    }
    followRedirects = true
}
