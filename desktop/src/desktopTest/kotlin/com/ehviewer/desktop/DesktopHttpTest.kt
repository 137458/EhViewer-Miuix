package com.ehviewer.desktop

import com.ehviewer.core.network.EhCookieStore
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.ktor.http.Url
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class DesktopHttpTest {
    private fun withServer(handler: (HttpExchange) -> Unit, block: (String) -> Unit) {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            try {
                handler(exchange)
            } finally {
                exchange.close()
            }
        }
        server.start()
        try {
            block("http://127.0.0.1:${server.address.port}")
        } finally {
            server.stop(0)
        }
    }

    private fun HttpExchange.respond(status: Int, body: String, headers: Map<String, String> = emptyMap()) {
        headers.forEach { (name, value) -> responseHeaders.add(name, value) }
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        sendResponseHeaders(status, bytes.size.toLong())
        responseBody.write(bytes)
    }

    @Test
    fun toByteBufferReturnsFlippedBufferForSuccess() {
        val buffer = DesktopResponse(200, "hello").toByteBuffer()
        assertEquals(5, buffer!!.remaining())
        val bytes = ByteArray(5)
        buffer.get(bytes)
        assertEquals("hello", String(bytes))
        // 直接缓冲带 headroom：容量大于真实长度，但 limit/remaining 不虚报
        assertEquals(5 + BYTE_BUFFER_HEADROOM, buffer.capacity())
        assertEquals(5, buffer.limit())
    }

    @Test
    fun toByteBufferNullForNonSuccessStatus() {
        assertNull(DesktopResponse(404, "gone").toByteBuffer())
        assertNull(DesktopResponse(500, "boom").toByteBuffer())
    }

    @Test
    fun redirectLocationResolvedAsHttpUrlOnly() {
        // 相对 Location 按当前 URL 解析
        assertEquals("http://host/b", resolveRedirectUrl("http://host/a", "/b"))
        // 相对引用按 RFC 3986 相对当前路径解析：/a/b + c = /a/c
        assertEquals("http://host/a/c", resolveRedirectUrl("http://host/a/b", "c"))
        assertEquals("https://other/x", resolveRedirectUrl("http://host/a", "https://other/x"))
        // 非 http(s) 目标（脚本协议、file、ftp）一律不跟随
        assertNull(resolveRedirectUrl("http://host/a", "javascript:alert(1)"))
        assertNull(resolveRedirectUrl("http://host/a", "file:///C:/x"))
        assertNull(resolveRedirectUrl("http://host/a", "ftp://host/x"))
        assertNull(resolveRedirectUrl("http://host/a", "::::"))
    }

    @Test
    fun redirectIsFollowedManually() {
        withServer(
            handler = { exchange ->
                when (exchange.requestURI.path) {
                    "/a" -> exchange.respond(302, "", mapOf("Location" to "/b"))
                    "/b" -> exchange.respond(200, "ok")
                    else -> exchange.respond(404, "nope")
                }
            },
        ) { base ->
            val response = performDesktopRequest("$base/a")
            assertEquals(200, response.status)
            assertEquals("ok", String(response.bytes, StandardCharsets.UTF_8))
        }
    }

    @Test
    fun redirectChainStopsAtHopLimit() {
        withServer(
            handler = { exchange ->
                val path = exchange.requestURI.path
                when (path) {
                    "/0" -> exchange.respond(302, "", mapOf("Location" to "/1"))
                    "/1" -> exchange.respond(302, "", mapOf("Location" to "/2"))
                    else -> exchange.respond(200, "end")
                }
            },
        ) { base ->
            // 只允许 1 跳：第二跳不再跟随，按当时的 3xx 返回空体（调用方判为失败）
            val limited = performDesktopRequest("$base/0", maxRedirects = 1)
            assertEquals(302, limited.status)
            assertEquals(0, limited.bytes.size)
            // 链路在允许范围内时能走到终点
            val full = performDesktopRequest("$base/0")
            assertEquals(200, full.status)
        }
    }

    @Test
    fun nonHttpRedirectTargetIsNotFollowed() {
        withServer(
            handler = { exchange ->
                exchange.respond(302, "", mapOf("Location" to "ftp://example.com/x"))
            },
        ) { base ->
            val response = performDesktopRequest("$base/a")
            assertEquals(302, response.status)
            assertEquals(0, response.bytes.size)
        }
    }

    @Test
    fun setCookieIsPersistedIntoCookieStore() {
        runBlocking { EhCookieStore.removeAllCookies() }
        try {
            withServer(
                handler = { exchange ->
                    exchange.respond(200, "logged-in", mapOf("Set-Cookie" to "ipb_member_id=42; Path=/"))
                },
            ) { base ->
                performDesktopRequest("$base/login")
                val cookies = runBlocking { EhCookieStore.get(Url("$base/")) }
                val stored = cookies.firstOrNull { it.name == "ipb_member_id" }
                assertEquals("42", stored?.value)
            }
        } finally {
            runBlocking { EhCookieStore.removeAllCookies() }
            runBlocking { EhCookieStore.flush() }
        }
    }

    @Test
    fun oversizedResponseIsRejected() {
        withServer(
            handler = { exchange -> exchange.respond(200, "x".repeat(5000)) },
        ) { base ->
            val error = assertFailsWith<IOException> {
                performDesktopRequest("$base/big", maxBytes = 1024L)
            }
            assertTrue(error.message.orEmpty().contains("上限"), "错误信息应说明上限，实际: ${error.message}")
        }
    }

    @Test
    fun responseWithinLimitIsReadCompletely() {
        withServer(
            handler = { exchange -> exchange.respond(200, "y".repeat(1000)) },
        ) { base ->
            val response = performDesktopRequest("$base/ok", maxBytes = 1024L)
            assertEquals(1000, response.bytes.size)
        }
    }

    @Test
    fun charsetParsedFromContentType() {
        assertEquals(StandardCharsets.UTF_8, charsetOf(null))
        assertEquals(StandardCharsets.UTF_8, charsetOf("text/html"))
        assertEquals(Charset.forName("GBK"), charsetOf("text/html; charset=GBK"))
        // 带引号与额外参数
        assertEquals(Charset.forName("UTF-8"), charsetOf("application/json; charset=\"utf-8\"; x=1"))
        // 非法字符集名回落 UTF-8，不抛异常
        assertEquals(StandardCharsets.UTF_8, charsetOf("text/html; charset=not-a-charset"))
    }

    @Test
    fun setCookieParserRejectsGarbage() {
        val cookie = parseSetCookie("ipb_member_id=42; Path=/")
        assertEquals("ipb_member_id", cookie?.name)
        assertEquals("42", cookie?.value)
        assertNull(parseSetCookie(""))
    }
}
