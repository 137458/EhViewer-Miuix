package com.ehviewer.desktop

import com.ehviewer.core.network.EhCookieStore
import java.io.File
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.URL

class DesktopResponse(val status: Int, val body: String)

// 代理解析：设置项 > HTTPS_PROXY/HTTP_PROXY 环境变量 > 直连（与 NetworkModule 同源规则）
private fun resolveProxy(): Proxy? {
    DesktopSettings.proxy.value?.trim()?.takeIf { it.isNotEmpty() }?.let { configured ->
        parseHostPort(configured)?.let { return Proxy(Proxy.Type.HTTP, it) }
    }
    val fromEnv = System.getenv("HTTPS_PROXY") ?: System.getenv("HTTP_PROXY")
    if (!fromEnv.isNullOrBlank()) {
        parseHostPort(fromEnv.trim())?.let { return Proxy(Proxy.Type.HTTP, it) }
    }
    return null
}

private fun parseHostPort(value: String): InetSocketAddress? {
    val match = Regex("^(https?://)?([^:/]+):(\\d+)$").matchEntire(value) ?: return null
    val (scheme, host, port) = match.destructured
    if (!scheme.isBlank() && !scheme.startsWith("http")) return null
    return runCatching { InetSocketAddress(host, port.toInt()) }.getOrNull()
}

// okhttp 与部分代理隧道存在同步挂死（探针实测，连超时都不触发），
// 桌面外部请求走 JDK HttpURLConnection（实测 576ms 可达）+ EhCookieStore Cookie 头注入。
fun desktopGet(url: String): DesktopResponse {
    val target = URL(url)
    val proxy = resolveProxy()
    val conn = (if (proxy != null) target.openConnection(proxy) else target.openConnection(Proxy.NO_PROXY))
        as HttpURLConnection
    conn.connectTimeout = 15_000
    conn.readTimeout = 15_000
    conn.instanceFollowRedirects = true
    conn.setRequestProperty(
        "User-Agent",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
    )
    conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
    conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
    val cookies = kotlinx.coroutines.runBlocking {
        EhCookieStore.get(io.ktor.http.Url(url))
    }.takeIf { it.isNotEmpty() }?.joinToString("; ") { "${it.name}=${it.value}" }
    cookies?.let { conn.setRequestProperty("Cookie", it) }
    val status = conn.responseCode
    val stream = if (status in 200..299) conn.inputStream else conn.errorStream
    val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
    return DesktopResponse(status, body)
}

// 诊断辅助：桌面数据目录（与 DesktopDirs 约定一致的只读入口）
internal fun desktopDataFileMarker(): File? = null
