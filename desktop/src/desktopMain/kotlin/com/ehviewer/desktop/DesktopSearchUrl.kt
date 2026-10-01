package com.ehviewer.desktop

import java.net.URLEncoder

// 站点搜索页 URL 构造（桌面在线搜索）；空白查询返回 null 表示不发起搜索
object DesktopSearchUrl {
    fun build(query: String): String? {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return null
        return "https://e-hentai.org/?f_search=" + URLEncoder.encode(trimmed, "UTF-8")
    }
}
