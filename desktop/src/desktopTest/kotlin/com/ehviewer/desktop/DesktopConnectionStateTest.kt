package com.ehviewer.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopConnectionStateTest {

    @Test
    fun statusTypeChecks_distinguishCheckingOnlineAndOffline() {
        val checking = DesktopConnectionStatus.Checking
        val online = DesktopConnectionStatus.Online(200)
        val offline = DesktopConnectionStatus.Offline("Timeout")

        assertTrue(DesktopConnectionState.isChecking(checking))
        assertFalse(DesktopConnectionState.isChecking(online))
        assertFalse(DesktopConnectionState.isChecking(offline))

        assertTrue(DesktopConnectionState.isOnline(online))
        assertFalse(DesktopConnectionState.isOnline(checking))
        assertFalse(DesktopConnectionState.isOnline(offline))

        assertTrue(DesktopConnectionState.isOffline(offline))
        assertFalse(DesktopConnectionState.isOffline(checking))
        assertFalse(DesktopConnectionState.isOffline(online))
    }

    @Test
    fun canRetry_allowedWhenNotChecking() {
        assertFalse(DesktopConnectionState.canRetry(DesktopConnectionStatus.Checking))
        assertTrue(DesktopConnectionState.canRetry(DesktopConnectionStatus.Online(200)))
        assertTrue(DesktopConnectionState.canRetry(DesktopConnectionStatus.Offline("Timeout")))
    }

    @Test
    fun formatStatus_formatsWithInjectedLocalizedLabels() {
        // 界面层经 i18n 注入标签；此处用等价英文断言纯逻辑（HTTP 状态码为协议文本）
        assertEquals(
            "Checking...",
            DesktopConnectionState.formatStatus(DesktopConnectionStatus.Checking, checkingLabel = "Checking...", offlineLabel = "Offline"),
        )
        assertEquals(
            "HTTP 200",
            DesktopConnectionState.formatStatus(DesktopConnectionStatus.Online(200), checkingLabel = "Checking...", offlineLabel = "Offline"),
        )
        assertEquals(
            "HTTP 503",
            DesktopConnectionState.formatStatus(DesktopConnectionStatus.Online(503), checkingLabel = "Checking...", offlineLabel = "Offline"),
        )

        assertEquals(
            "Offline (Connection refused)",
            DesktopConnectionState.formatStatus(DesktopConnectionStatus.Offline("Connection refused"), checkingLabel = "Checking...", offlineLabel = "Offline"),
        )
        // 边界：空原因或空白原因不带括号
        assertEquals(
            "Offline",
            DesktopConnectionState.formatStatus(DesktopConnectionStatus.Offline(""), checkingLabel = "Checking...", offlineLabel = "Offline"),
        )
        assertEquals(
            "Offline",
            DesktopConnectionState.formatStatus(DesktopConnectionStatus.Offline("   "), checkingLabel = "Checking...", offlineLabel = "Offline"),
        )

        // 中文标签注入等价成立
        assertEquals(
            "离线 (Connection refused)",
            DesktopConnectionState.formatStatus(DesktopConnectionStatus.Offline("Connection refused"), checkingLabel = "检查中", offlineLabel = "离线"),
        )
    }

    @Test
    fun cleanErrorMessage_parsesAndSanitizesErrors() {
        // 界面层经 i18n 注入标签；此处用等价英文断言纯逻辑
        val labels = DesktopConnectionErrorLabels(
            networkUnavailable = "Network unavailable",
            dnsUnresolved = "DNS / Host unresolved",
            connectionRefused = "Connection refused",
            connectionTimedOut = "Connection timed out",
            sslHandshakeFailed = "SSL handshake failed",
        )

        // 边界：null 或 blank
        assertEquals("Network unavailable", DesktopConnectionState.cleanErrorMessage(null, labels))
        assertEquals("Network unavailable", DesktopConnectionState.cleanErrorMessage("", labels))
        assertEquals("Network unavailable", DesktopConnectionState.cleanErrorMessage("   ", labels))

        // DNS / Host 解析失败
        assertEquals(
            "DNS / Host unresolved",
            DesktopConnectionState.cleanErrorMessage("java.net.UnknownHostException: e-hentai.org", labels),
        )
        assertEquals(
            "DNS / Host unresolved",
            DesktopConnectionState.cleanErrorMessage("Unable to resolve host \"e-hentai.org\": No address associated with hostname", labels),
        )

        // 连接拒绝
        assertEquals(
            "Connection refused",
            DesktopConnectionState.cleanErrorMessage("java.net.ConnectException: Failed to connect to /104.20.134.21:443", labels),
        )

        // 超时
        assertEquals(
            "Connection timed out",
            DesktopConnectionState.cleanErrorMessage("java.net.SocketTimeoutException: timeout", labels),
        )
        assertEquals(
            "Connection timed out",
            DesktopConnectionState.cleanErrorMessage("connect timed out", labels),
        )

        // SSL 握手失败
        assertEquals(
            "SSL handshake failed",
            DesktopConnectionState.cleanErrorMessage("javax.net.ssl.SSLHandshakeException: Handshake failed", labels),
        )

        // 中文标签注入等价成立
        val zhLabels = labels.copy(
            networkUnavailable = "网络不可用",
            connectionRefused = "连接被拒绝",
        )
        assertEquals("连接被拒绝", DesktopConnectionState.cleanErrorMessage("java.net.ConnectException: refused", zhLabels))
        assertEquals("网络不可用", DesktopConnectionState.cleanErrorMessage(null, zhLabels))

        // 通用异常剥离类名前缀与超长截断
        assertEquals(
            "Stream closed",
            DesktopConnectionState.cleanErrorMessage("java.io.IOException: Stream closed", labels),
        )
        val longError = "An extremely long and verbose error message that exceeds forty characters completely"
        val cleanedLong = DesktopConnectionState.cleanErrorMessage(longError, labels)
        assertTrue(cleanedLong.endsWith("..."))
        assertTrue(cleanedLong.length <= 40)
    }
}
