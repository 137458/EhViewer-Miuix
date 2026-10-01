package com.ehviewer.desktop

import com.ehviewer.core.util.LogPriority
import com.ehviewer.core.util.logcat
import java.awt.Desktop
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

    fun openUrl(url: String, opener: (URI) -> Unit = { Desktop.getDesktop().browse(it) }): Boolean {
        if (!isValidHttpUrl(url)) return false
        return runCatching {
            opener(URI(url.trim()))
            true
        }.onFailure { e ->
            logcat("DesktopBrowser", LogPriority.WARN) { "Failed to open url: ${e.message}" }
        }.getOrElse { false }
    }
}
