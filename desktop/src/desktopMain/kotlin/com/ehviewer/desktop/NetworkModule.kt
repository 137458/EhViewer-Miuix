package com.ehviewer.desktop

import com.ehviewer.core.network.EhCookieStore
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.userAgent
import java.net.InetSocketAddress
import java.net.ProxySelector

// 桌面网络栈统一 UA（HttpURLConnection 与 ktor okhttp 共用）；版本随 Chrome 主版本季更
internal const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36"
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

private fun buildClient(proxySelector: ProxySelector?): HttpClient = when (proxySelector) {
    // okhttp 与部分代理隧道在 selector 路径上存在同步挂死（探针实测），代理场景改用 CIO 引擎
    null -> buildPlainClient()
    else -> HttpClient(CIO) { configureDesktopCommon() }
}

private fun HttpClientConfig<*>.configureDesktopCommon() {
    expectSuccess = false
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

private fun buildPlainClient(): HttpClient = HttpClient(OkHttp) {
    expectSuccess = false
    engine {
        config {
            // 部分代理隧道对 ALPN h2 协商处理不佳，桌面侧固定 HTTP/1.1
            protocols(listOf(okhttp3.Protocol.HTTP_1_1))
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
