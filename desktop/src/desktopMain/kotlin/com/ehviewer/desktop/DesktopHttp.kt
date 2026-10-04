package com.ehviewer.desktop

import com.ehviewer.core.network.EhCookieStore
import java.io.File
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.URL
import java.nio.ByteBuffer

class DesktopResponse(val status: Int, val body: String)

class DesktopBytesResponse(val status: Int, val bytes: ByteArray)

// 2xx 响应体转直读 ByteBuffer（Rust 解析入口约定 flip 后的 Direct buffer）；非 2xx 返回 null
fun DesktopResponse.toByteBuffer(): ByteBuffer? {
    if (status !in 200..299) return null
    val bytes = body.toByteArray()
    return ByteBuffer.allocateDirect(bytes.size).put(bytes).apply { flip() }
}

// 代理解析：设置项 > HTTPS_PROXY/HTTP_PROXY 环境变量 > 直连（供 HttpURLConnection 与 NetworkModule 共享）
internal fun resolveDesktopProxyAddress(): InetSocketAddress? {
    DesktopSettings.proxy.value?.trim()?.takeIf { it.isNotEmpty() }?.let { configured ->
        parseHostPort(configured)?.let { return it }
    }
    val fromEnv = System.getenv("HTTPS_PROXY") ?: System.getenv("HTTP_PROXY")
    if (!fromEnv.isNullOrBlank()) {
        parseHostPort(fromEnv.trim())?.let { return it }
    }
    return null
}

private fun resolveProxy(): Proxy? = resolveDesktopProxyAddress()?.let { Proxy(Proxy.Type.HTTP, it) }

internal fun parseHostPort(value: String): InetSocketAddress? {
    val match = Regex("^(https?://)?([^:/]+):(\\d+)$").matchEntire(value) ?: return null
    val (scheme, host, port) = match.destructured
    if (!scheme.isBlank() && !scheme.startsWith("http")) return null
    return runCatching { InetSocketAddress(host, port.toInt()) }.getOrNull()
}

// okhttp 与部分代理隧道存在同步挂死（探针实测，连超时都不触发），桌面外部请求走 JDK
// HttpURLConnection（实测 576ms 可达）+ EhCookieStore Cookie 头注入。
private fun openDesktopConnection(url: String, method: String, body: String?): HttpURLConnection {
    val target = URL(url)
    val proxy = resolveProxy()
    val conn = (if (proxy != null) target.openConnection(proxy) else target.openConnection(Proxy.NO_PROXY))
        as HttpURLConnection
    conn.requestMethod = method
    conn.connectTimeout = 15_000
    conn.readTimeout = 15_000
    conn.instanceFollowRedirects = true
    conn.setRequestProperty(
        "User-Agent",
        DESKTOP_USER_AGENT,
    )
    conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
    conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
    val cookies = kotlinx.coroutines.runBlocking {
        EhCookieStore.get(io.ktor.http.Url(url))
    }.takeIf { it.isNotEmpty() }?.joinToString("; ") { "${it.name}=${it.value}" }
    cookies?.let { conn.setRequestProperty("Cookie", it) }
    if (body != null) {
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.outputStream.use { it.write(body.toByteArray()) }
    }
    return conn
}

fun desktopGet(url: String): DesktopResponse = readDesktopResponse(openDesktopConnection(url, "GET", null))

// 图片等二进制资源抓取：desktopGet 的 String body 经字符解码会损坏字节，二进制走本入口
fun desktopGetBytes(url: String): DesktopBytesResponse = readDesktopBytesResponse(openDesktopConnection(url, "GET", null))

// gdata 等 JSON API 的 POST 入口，代理/Cookie/UA 规则与 desktopGet 完全一致
fun desktopPost(url: String, body: String): DesktopResponse = readDesktopResponse(openDesktopConnection(url, "POST", body))

private fun readDesktopResponse(conn: HttpURLConnection): DesktopResponse {
    try {
        val status = conn.responseCode
        val stream = if (status in 200..299) conn.inputStream else conn.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        return DesktopResponse(status, body)
    } finally {
        conn.disconnect()
    }
}

private fun readDesktopBytesResponse(conn: HttpURLConnection): DesktopBytesResponse {
    try {
        val status = conn.responseCode
        val stream = if (status in 200..299) conn.inputStream else conn.errorStream
        val bytes = stream?.use { it.readBytes() } ?: ByteArray(0)
        return DesktopBytesResponse(status, bytes)
    } finally {
        conn.disconnect()
    }
}
