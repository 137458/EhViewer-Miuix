package com.ehviewer.desktop

import com.ehviewer.core.util.LogPriority
import com.ehviewer.core.util.logcat
import java.awt.Desktop
import java.lang.ProcessBuilder
import java.net.URI

object DesktopBrowser {
    fun isValidHttpUrl(url: String): Boolean {
        val trimmed = url.trim()
        val isHttp = trimmed.startsWith("http://", ignoreCase = true)
        val isHttps = trimmed.startsWith("https://", ignoreCase = true)
        if (!isHttp && !isHttps) return false
        val prefixLen = if (isHttps) 8 else 7
        return trimmed.length > prefixLen
    }

    fun openUrl(
        url: String,
        // 生产默认真实拉起进程；测试注入记录型 lambda（空实现会使回退假成功）
        launcher: (List<String>) -> Unit = { commands -> ProcessBuilder(commands).start() },
        osName: String = System.getProperty("os.name", ""),
        opener: (URI) -> Unit = { Desktop.getDesktop().browse(it) },
    ): Boolean {
        if (!isValidHttpUrl(url)) return false
        val browse = runCatching {
            opener(URI(url.trim()))
        }
        if (browse.isSuccess) return true
        browse.onFailure { e ->
            logcat("DesktopBrowser", LogPriority.WARN) { "Failed to open url: ${e.message}" }
        }
        // AWT 浏览器关联失败（UWP 默认浏览器等场景）的 Windows shell 回退
        val fallback = fallbackCommand(url, osName) ?: return false
        return runCatching {
            launcher(fallback)
            true
        }.onFailure { e ->
            logcat("DesktopBrowser", LogPriority.WARN) { "Fallback open failed: ${e.message}" }
        }.getOrElse { false }
    }

    // Windows shell 兜底：rundll32 url.dll 协议处理器不依赖 AWT 关联；非 Windows 无兜底
    fun fallbackCommand(url: String, osName: String = System.getProperty("os.name", "")): List<String>? = if (osName.contains("windows", ignoreCase = true)) {
        listOf("rundll32", "url.dll,FileProtocolHandler", url.trim())
    } else {
        null
    }
}
