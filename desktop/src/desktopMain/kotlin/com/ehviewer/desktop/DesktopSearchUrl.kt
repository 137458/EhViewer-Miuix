package com.ehviewer.desktop

import java.net.URLEncoder

// 站点搜索页 URL 构造（桌面在线搜索）；空白查询返回 null 表示不发起搜索。
// page 从 0 起：0 或负值（第一页）不带 page 参数，与站点约定一致。
object DesktopSearchUrl {
    fun build(query: String, page: Int = 0): String? {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return null
        val base = "https://e-hentai.org/?f_search=" + URLEncoder.encode(trimmed, "UTF-8")
        return if (page > 0) "$base&page=$page" else base
    }
}
