package com.ehviewer.desktop

import com.ehviewer.core.network.EhCookieStore
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.userAgent
import java.net.InetSocketAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 桌面网络栈统一 UA（HttpURLConnection 与 ktor okhttp 共用）；版本随 Chrome 主版本季更
internal const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36"
private const val CHROME_ACCEPT =
    "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8"

// 桌面网络栈：与 Android 侧共享 EhCookieStore(ktor CookiesStorage)，
// 引擎用 okhttp（Cronet 仅 Android 可用），UA 固定桌面 Chrome。
// 代理在设置页修改后自动重建客户端。
private var clientCache: HttpClient? = null
private var clientProxyKey: String? = null
private val clientLock = Any()

// 旧客户端回收协程域：只用于延迟 close，不承载业务请求
private val retireScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
private const val CLIENT_RETIRE_DELAY_MS = 30_000L

fun acquireClient(): HttpClient = synchronized(clientLock) {
    val proxyKey = DesktopSettings.proxy.value
    val cached = clientCache
    if (cached == null || clientProxyKey != proxyKey) {
        clientCache = buildClient(proxyAddress = resolveDesktopProxyAddress())
        clientProxyKey = proxyKey
        // 不在代理切换时同步 close：切换瞬间可能仍有在飞的请求，close() 会直接掐断它们
        // （表现为图片/列表加载无端失败），交给延迟回收让在飞请求自然收尾
        cached?.let { retireClient(it) }
    }
    clientCache!!
}

// 代理地址与 DesktopHttp 共用同一解析结果（设置项 > 环境变量 > 直连）
internal fun buildClient(proxyAddress: InetSocketAddress?): HttpClient = when (proxyAddress) {
    // okhttp 与部分代理隧道在 selector 路径上存在同步挂死（探针实测），代理场景改用 CIO 引擎，
    // 并把代理钉进 engine（CIO 不走 JVM ProxySelector）
    null -> buildPlainClient()
    else -> HttpClient(CIO) {
        engine {
            proxy = ProxyBuilder.http(Url("http://${proxyAddress.hostString}:${proxyAddress.port}"))
        }
        configureDesktopCommon()
    }
}

private fun retireClient(client: HttpClient) {
    retireScope.launch {
        delay(CLIENT_RETIRE_DELAY_MS)
        client.close()
    }
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
