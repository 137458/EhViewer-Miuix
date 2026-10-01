package com.ehviewer.desktop

import java.net.URLEncoder

// 站点搜索页 URL 构造（桌面在线搜索）。
// 站点分页为游标式：next = 当前结果最后一项 gid；prev=1 语义为回到第一页。
// 空白查询返回 null 表示不发起搜索。
object DesktopSearchUrl {
    fun build(query: String, nextGid: Long? = null): String? {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return null
        val base = "https://e-hentai.org/?f_search=" + URLEncoder.encode(trimmed, "UTF-8")
        return if (nextGid != null && nextGid >= 0) "$base&next=$nextGid" else base
    }

    fun buildFirstPage(query: String): String? {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return null
        return "https://e-hentai.org/?f_search=" + URLEncoder.encode(trimmed, "UTF-8") + "&prev=1"
    }
}
