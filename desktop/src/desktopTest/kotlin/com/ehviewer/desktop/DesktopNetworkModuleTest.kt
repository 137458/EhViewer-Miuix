package com.ehviewer.desktop

import com.sun.net.httpserver.HttpServer
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class DesktopNetworkModuleTest {
    private fun <T> withServer(block: (HttpServer, MutableList<String>) -> T): T {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val requests = mutableListOf<String>()
        server.createContext("/") { exchange ->
            requests += exchange.requestURI.toString()
            val body = "ok".toByteArray(StandardCharsets.UTF_8)
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.write(body)
            exchange.close()
        }
        server.start()
        try {
            return block(server, requests)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun directClientReachesOriginWithoutProxy() = withServer { server, requests ->
        val client = buildClient(proxyAddress = null)
        try {
            val body = runBlocking { client.get("http://127.0.0.1:${server.address.port}/direct").bodyAsText() }
            assertEquals("ok", body)
            // 直连时请求行是 origin-form（只要路径，不带 scheme/host）
            assertEquals(listOf("/direct"), requests)
        } finally {
            client.close()
        }
    }

    @Test
    fun proxyClientRoutesAbsoluteUriThroughConfiguredProxy() = withServer { proxy, requests ->
        // 代理场景走 CIO 引擎并把代理钉进 engine：CIO 不做 JVM ProxySelector，
        // 若未真正生效，请求会直接去 example.invalid 而不是到达本地代理
        val client = buildClient(proxyAddress = InetSocketAddress("127.0.0.1", proxy.address.port))
        try {
            val body = runBlocking { client.get("http://example.invalid/page?a=1").bodyAsText() }
            assertEquals("ok", body)
            assertTrue(
                requests.singleOrNull()?.contains("example.invalid") == true,
                "代理应收到 absolute-form 请求行，实际: $requests",
            )
        } finally {
            client.close()
        }
    }
}
