package com.ehviewer.desktop

import com.ehviewer.core.network.EhCookieStore
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.net.URISyntaxException
import java.nio.ByteBuffer
import java.nio.charset.Charset

class DesktopResponse(val status: Int, val body: String)

class DesktopBytesResponse(val status: Int, val bytes: ByteArray)

// 手工跟随重定向：HttpURLConnection 自动跳转会把原域 Cookie 原样带到新域（跨域泄漏），
// 且无法在中途改写/丢弃 Cookie，故统一由本层每跳重查 EhCookieStore。
internal const val MAX_REDIRECTS = 5

// 响应体上限：文本 16MB（页面/JSON），二进制 64MB（图片），超限直接失败而非静默截断
internal const val MAX_TEXT_RESPONSE_BYTES = 16L * 1024 * 1024
internal const val MAX_BYTES_RESPONSE_BYTES = 64L * 1024 * 1024

// Direct buffer 预留容量（limit 仍为真实长度）：Rust 侧解析器按容量侧读取时不会越界
internal const val BYTE_BUFFER_HEADROOM = 64

internal val REDIRECT_STATUS_CODES = setOf(301, 302, 303, 307, 308)

// 2xx 响应体转直读 ByteBuffer（Rust 解析入口约定 flip 后的 Direct buffer）；非 2xx 返回 null
fun DesktopResponse.toByteBuffer(): ByteBuffer? {
    if (status !in 200..299) return null
    val bytes = body.toByteArray()
    return ByteBuffer.allocateDirect(bytes.size + BYTE_BUFFER_HEADROOM).put(bytes).apply { flip() }
}

// 代理解析：设置项 > HTTPS_PROXY/HTTP_PROXY（含小写形态）环境变量 > 直连
// 供 HttpURLConnection（DesktopHttp）与 ktor（NetworkModule / DesktopImageLoader）共享同一结果
internal fun resolveDesktopProxyAddress(
    configured: String? = DesktopSettings.proxy.value,
    httpsProxy: String? = System.getenv("HTTPS_PROXY") ?: System.getenv("https_proxy"),
    httpProxy: String? = System.getenv("HTTP_PROXY") ?: System.getenv("http_proxy"),
): InetSocketAddress? {
    configured?.trim()?.takeIf { it.isNotEmpty() }?.let { return parseHostPort(it) }
    httpsProxy?.trim()?.takeIf { it.isNotEmpty() }?.let { return parseHostPort(it) }
    httpProxy?.trim()?.takeIf { it.isNotEmpty() }?.let { return parseHostPort(it) }
    return null
}

// 代理地址解析：接受 host:port、scheme://host:port、带尾部斜杠（http://host:8080/）、
// IPv6 字面量（[::1]:8080，走 URI 取出 host），scheme 只认 http/https
internal fun parseHostPort(value: String): InetSocketAddress? {
    val trimmed = value.trim().trimEnd('/')
    if (trimmed.isEmpty()) return null
    val candidate = if (trimmed.contains("://")) trimmed else "http://$trimmed"
    val uri = try {
        URI(candidate)
    } catch (e: URISyntaxException) {
        return null
    }
    val scheme = uri.scheme?.lowercase() ?: return null
    if (scheme != "http" && scheme != "https") return null
    val host = uri.host ?: return null
    if (host.isBlank()) return null
    val port = if (uri.port > 0) {
        uri.port
    } else if (scheme == "https") {
        443
    } else {
        80
    }
    if (port !in 1..65535) return null
    return try {
        InetSocketAddress(host, port)
    } catch (e: IllegalArgumentException) {
        // 端口越界/主机名非法：URI 已拦大部分，这里兜住剩下的非法组合
        null
    }
}

// Location 可能是相对路径：以当前请求 URI 为基准归一化，且只放行 http/https（防 file/javascript 跳转）
internal fun resolveRedirectUrl(currentUrl: String, location: String): String? {
    val target = location.trim()
    if (target.isEmpty()) return null
    val resolved = try {
        URI(currentUrl).resolve(target)
    } catch (e: URISyntaxException) {
        return null
    } catch (e: IllegalArgumentException) {
        return null
    }
    return when (resolved?.scheme?.lowercase()) {
        "http", "https" -> resolved.toString()
        else -> null
    }
}

// Content-Type 里的 charset 优先，缺失/无法识别时按 UTF-8（e-hentai 全站 UTF-8；
// 旧实现用平台默认字符集，在 GBK 等非 UTF-8 默认的 Windows 上会把页面解成乱码）
internal fun charsetOf(contentType: String?): Charset {
    val name = contentType?.substringAfter("charset=", "")?.substringBefore(';')?.trim()?.trim('"')
    if (name.isNullOrEmpty()) return Charsets.UTF_8
    return try {
        Charset.forName(name)
    } catch (e: IllegalArgumentException) {
        Charsets.UTF_8
    }
}

// okhttp 与部分代理隧道存在同步挂死（探针实测，连超时都不触发），桌面外部请求走 JDK
// HttpURLConnection（实测 576ms 可达）+ EhCookieStore Cookie 头注入。
private fun openDesktopConnection(url: String, method: String, body: String?): HttpURLConnection {
    val target = URI(url).toURL()
    val proxy = resolveDesktopProxyAddress()?.let { Proxy(Proxy.Type.HTTP, it) }
    val conn = (if (proxy != null) target.openConnection(proxy) else target.openConnection(Proxy.NO_PROXY))
        as HttpURLConnection
    conn.requestMethod = method
    conn.connectTimeout = 15_000
    conn.readTimeout = 15_000
    // 重定向由 performRequest 手工跟随：自动跳转会把原域 Cookie 带到新域
    conn.instanceFollowRedirects = false
    conn.useCaches = false
    conn.setRequestProperty("User-Agent", DESKTOP_USER_AGENT)
    conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
    conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
    requestCookieHeader(url)?.let { conn.setRequestProperty("Cookie", it) }
    if (body != null) {
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.outputStream.use { it.write(body.toByteArray()) }
    }
    return conn
}

// 每跳单独查询 Cookie：目标是新域时只带新域匹配的 Cookie，避免跨域泄漏
private fun requestCookieHeader(url: String): String? {
    val ktorUrl = try {
        io.ktor.http.Url(url)
    } catch (e: IllegalArgumentException) {
        return null
    }
    val cookies = kotlinx.coroutines.runBlocking { EhCookieStore.get(ktorUrl) }
    if (cookies.isEmpty()) return null
    return cookies.joinToString("; ") { "${it.name}=${it.value}" }
}

// Set-Cookie 回写：JDK HttpURLConnection 不维护 Cookie 罐，登录态/CF 校验依赖这一步
private fun storeResponseCookies(url: String, conn: HttpURLConnection) {
    val rawCookies = conn.headerFields.entries
        .filter { (name, _) -> name?.equals("Set-Cookie", ignoreCase = true) == true }
        .flatMap { it.value.orEmpty() }
        .filter { it.isNotBlank() }
    if (rawCookies.isEmpty()) return
    val ktorUrl = try {
        io.ktor.http.Url(url)
    } catch (e: IllegalArgumentException) {
        return
    }
    val parsed = rawCookies.mapNotNull { parseSetCookie(it) }
    if (parsed.isEmpty()) return
    kotlinx.coroutines.runBlocking {
        parsed.forEach { EhCookieStore.addCookie(ktorUrl, it) }
    }
    runCatching { EhCookieStore.flush() }
}

internal fun parseSetCookie(raw: String): io.ktor.http.Cookie? = try {
    io.ktor.http.parseServerSetCookieHeader(raw)
} catch (e: RuntimeException) {
    // ktor 解析器对畸形值抛 IllegalArgumentException 或 NoSuchElementException（如空串）
    null
}

// 手工跟随重定向后的最终响应
internal class DesktopRawResponse(
    val status: Int,
    val bytes: ByteArray,
    val contentType: String?,
)

internal fun performDesktopRequest(
    url: String,
    method: String = "GET",
    body: String? = null,
    maxBytes: Long = MAX_TEXT_RESPONSE_BYTES,
    maxRedirects: Int = MAX_REDIRECTS,
): DesktopRawResponse {
    var currentUrl = url
    var currentMethod = method
    var currentBody = body
    var hops = 0
    while (true) {
        val conn = openDesktopConnection(currentUrl, currentMethod, currentBody)
        val status = try {
            conn.responseCode
        } catch (e: IOException) {
            conn.disconnect()
            throw e
        }
        try {
            storeResponseCookies(currentUrl, conn)
        } catch (e: IOException) {
            // Cookie 回写失败不应让整次请求失败
        }
        if (status in REDIRECT_STATUS_CODES) {
            val nextUrl = conn.getHeaderField("Location")?.let { resolveRedirectUrl(currentUrl, it) }
            conn.disconnect()
            // 畸形 Location 或超过跳数上限：不跟随，按当前状态返回空体（调用方判为失败）
            if (nextUrl == null || hops >= maxRedirects) {
                return DesktopRawResponse(status, ByteArray(0), null)
            }
            // 301/302/303 把 POST 降级为 GET；307/308 保留方法与请求体
            if (status != 307 && status != 308) {
                currentMethod = "GET"
                currentBody = null
            }
            currentUrl = nextUrl
            hops++
            continue
        }
        val contentType = conn.contentType
        val bytes = try {
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            stream?.use { readCapped(it, maxBytes) } ?: ByteArray(0)
        } finally {
            conn.disconnect()
        }
        return DesktopRawResponse(status, bytes, contentType)
    }
}

// 带上限读取：超限抛 IOException（失败要冒泡，不能静默截断后交给解析器）
private fun readCapped(stream: InputStream, maxBytes: Long): ByteArray {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    var total = 0L
    while (true) {
        val read = stream.read(buffer)
        if (read < 0) break
        total += read
        if (total > maxBytes) throw IOException("响应体超过 $maxBytes 字节上限")
        out.write(buffer, 0, read)
    }
    return out.toByteArray()
}

fun desktopGet(url: String): DesktopResponse {
    val raw = performDesktopRequest(url, maxBytes = MAX_TEXT_RESPONSE_BYTES)
    return DesktopResponse(raw.status, String(raw.bytes, charsetOf(raw.contentType)))
}

// 图片等二进制资源抓取：desktopGet 的 String body 经字符解码会损坏字节，二进制走本入口
fun desktopGetBytes(url: String): DesktopBytesResponse {
    val raw = performDesktopRequest(url, maxBytes = MAX_BYTES_RESPONSE_BYTES)
    return DesktopBytesResponse(raw.status, raw.bytes)
}

// gdata 等 JSON API 的 POST 入口，代理/Cookie/UA/重定向规则与 desktopGet 完全一致
fun desktopPost(url: String, body: String): DesktopResponse {
    val raw = performDesktopRequest(url, method = "POST", body = body, maxBytes = MAX_TEXT_RESPONSE_BYTES)
    return DesktopResponse(raw.status, String(raw.bytes, charsetOf(raw.contentType)))
}
