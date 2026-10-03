package com.ehviewer.desktop

sealed interface DesktopConnectionStatus {
    data object Checking : DesktopConnectionStatus
    data class Online(val statusCode: Int) : DesktopConnectionStatus
    data class Offline(val reason: String) : DesktopConnectionStatus
}

// 连接错误标签包：由界面层注入本地化文案，保持本对象无 i18n 依赖（与 formatStatus 的标签注入同模式）
data class DesktopConnectionErrorLabels(
    val networkUnavailable: String,
    val dnsUnresolved: String,
    val connectionRefused: String,
    val connectionTimedOut: String,
    val sslHandshakeFailed: String,
)

object DesktopConnectionState {
    fun cleanErrorMessage(rawMessage: String?, labels: DesktopConnectionErrorLabels): String {
        if (rawMessage.isNullOrBlank()) return labels.networkUnavailable
        val trimmed = rawMessage.trim()
        val firstLine = trimmed.lines().firstOrNull()?.trim() ?: ""
        return when {
            firstLine.contains("UnknownHostException", ignoreCase = true) ||
                firstLine.contains("Unable to resolve host", ignoreCase = true) ||
                firstLine.contains("No address associated with hostname", ignoreCase = true) ->
                labels.dnsUnresolved
            firstLine.contains("ConnectException", ignoreCase = true) ||
                firstLine.contains("Connection refused", ignoreCase = true) ->
                labels.connectionRefused
            firstLine.contains("SocketTimeoutException", ignoreCase = true) ||
                firstLine.contains("timed out", ignoreCase = true) ||
                firstLine.contains("TimeoutException", ignoreCase = true) ->
                labels.connectionTimedOut
            firstLine.contains("SSLHandshakeException", ignoreCase = true) ||
                firstLine.contains("CertPathValidatorException", ignoreCase = true) ||
                firstLine.contains("SSLPeerUnverifiedException", ignoreCase = true) ->
                labels.sslHandshakeFailed
            firstLine.contains("HttpException", ignoreCase = true) ||
                firstLine.contains("HTTP ", ignoreCase = true) -> {
                val cleaned = firstLine.replace(Regex("^[a-zA-Z0-9_.]*HttpException:\\s*"), "").trim()
                if (cleaned.length > 40) cleaned.take(37) + "..." else cleaned
            }
            else -> {
                val noPrefix = firstLine.replace(Regex("^[a-zA-Z0-9_.]*(Exception|Error):\\s*"), "").trim()
                if (noPrefix.length > 40) noPrefix.take(37) + "..." else noPrefix
            }
        }
    }

    // 状态文案由界面层经 i18n 提供标签（Checking/Offline），HTTP 状态码为协议文本不本地化
    fun formatStatus(
        status: DesktopConnectionStatus,
        checkingLabel: String,
        offlineLabel: String,
    ): String = when (status) {
        DesktopConnectionStatus.Checking -> checkingLabel
        is DesktopConnectionStatus.Online -> "HTTP ${status.statusCode}"
        is DesktopConnectionStatus.Offline -> {
            val trimmedReason = status.reason.trim()
            if (trimmedReason.isEmpty()) offlineLabel else "$offlineLabel ($trimmedReason)"
        }
    }

    fun canRetry(status: DesktopConnectionStatus): Boolean = status != DesktopConnectionStatus.Checking

    fun isOnline(status: DesktopConnectionStatus): Boolean = status is DesktopConnectionStatus.Online

    fun isOffline(status: DesktopConnectionStatus): Boolean = status is DesktopConnectionStatus.Offline

    fun isChecking(status: DesktopConnectionStatus): Boolean = status is DesktopConnectionStatus.Checking
}
