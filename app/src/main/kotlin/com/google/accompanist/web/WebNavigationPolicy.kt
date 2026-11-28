package com.google.accompanist.web

import java.net.URI

/**
 * WebView 顶层导航准入策略。
 *
 * 登录页与网页工具只应停留在 EH 相关站点的 HTTPS 页面上；其余 scheme
 * （`file://` / `content://` / `intent://` / `javascript:` / `data:` / `about:`）
 * 与第三方域名一律不在应用内加载，避免被远端页面走私本地私有文件读取或伪造内部流程。
 */
object WebNavigationPolicy {
    private val allowedHostSuffixes = listOf(
        "e-hentai.org",
        "exhentai.org",
        "ehgt.org",
        "hath.network",
        "ehwiki.org",
        "cloudflare.com",
        "hcaptcha.com",
    )

    fun shouldLoadInWebView(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        if (!"https".equals(uri.scheme, ignoreCase = true)) return false
        val host = uri.host?.lowercase() ?: return false
        return allowedHostSuffixes.any { suffix -> host == suffix || host.endsWith(".$suffix") }
    }
}
