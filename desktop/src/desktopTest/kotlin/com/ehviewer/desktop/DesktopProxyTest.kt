package com.ehviewer.desktop

import java.net.Inet6Address
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopProxyTest {
    @Test
    fun parseHostPortAcceptsCommonForms() {
        assertEquals("127.0.0.1", parseHostPort("http://127.0.0.1:8080")?.hostString)
        assertEquals(8080, parseHostPort("http://127.0.0.1:8080")?.port)
        // 无 scheme 视为 http
        assertEquals(8080, parseHostPort("127.0.0.1:8080")?.port)
        // 尾斜杠不参与解析
        assertEquals(3128, parseHostPort("http://proxy.local:3128/")?.port)
        // https 未写端口默认 443，http 默认 80
        assertEquals(443, parseHostPort("https://proxy.example")?.port)
        assertEquals(80, parseHostPort("proxy.example")?.port)
        // port=0 视为未指定
        assertEquals(80, parseHostPort("http://proxy.example:0")?.port)
    }

    @Test
    fun parseHostPortSupportsIpv6Literal() {
        val address = parseHostPort("http://[::1]:9090/")
        assertEquals(9090, address?.port)
        assertTrue(address?.address is Inet6Address, "IPv6 字面量应解析为 Inet6Address，实际: ${address?.address}")
    }

    @Test
    fun parseHostPortRejectsUnusableValues() {
        assertNull(parseHostPort(""))
        assertNull(parseHostPort("   "))
        // 非 http(s) 代理协议不受支持（CIO 只走 HTTP 代理）
        assertNull(parseHostPort("ftp://proxy:21"))
        assertNull(parseHostPort("socks5://proxy:1080"))
        // 端口越界
        assertNull(parseHostPort("http://proxy:70000"))
    }

    @Test
    fun configuredProxyWins() {
        val address = resolveDesktopProxyAddress(
            configured = "http://configured:1111",
            httpsProxy = "http://https-env:2222",
            httpProxy = "http://http-env:3333",
        )
        assertEquals("configured", address?.hostString)
        assertEquals(1111, address?.port)
    }

    @Test
    fun environmentFallbackOrderIsHttpsThenHttp() {
        val fromHttps = resolveDesktopProxyAddress(
            configured = null,
            httpsProxy = "http://https-env:2222",
            httpProxy = "http://http-env:3333",
        )
        assertEquals("https-env", fromHttps?.hostString)
        assertEquals(2222, fromHttps?.port)

        val fromHttp = resolveDesktopProxyAddress(
            configured = "   ",
            httpsProxy = "",
            httpProxy = "http://http-env:3333",
        )
        assertEquals("http-env", fromHttp?.hostString)
        assertEquals(3333, fromHttp?.port)
    }

    @Test
    fun noProxyConfiguredResolvesToDirect() {
        assertNull(resolveDesktopProxyAddress(configured = null, httpsProxy = null, httpProxy = null))
        assertNull(resolveDesktopProxyAddress(configured = "", httpsProxy = " ", httpProxy = ""))
        // 设置了非法值等同直连（不能把畸形串塞给引擎）
        assertNull(resolveDesktopProxyAddress(configured = "not a proxy", httpsProxy = null, httpProxy = null))
    }
}
