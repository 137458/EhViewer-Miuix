package com.ehviewer.desktop

import java.net.URLEncoder

// 站点搜索页 URL 构造（桌面在线搜索）。
// 站点分页为游标双向：next = 当前结果末位 gid（往新画廊方向）；prev = gid（往更早画廊方向）。
// 空白查询返回 null 表示不发起搜索。
object DesktopSearchUrl {
    fun build(query: String, nextGid: Long? = null, backward: Boolean = false): String? {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return null
        val base = "https://e-hentai.org/?f_search=" + URLEncoder.encode(trimmed, "UTF-8")
        val cursor = when {
            nextGid == null || nextGid < 0 -> return base
            backward -> "prev=$nextGid"
            else -> "next=$nextGid"
        }
        return "$base&$cursor"
    }
}

object DesktopOnlinePagination {
    fun canNavigatePrev(cursorIndex: Int): Boolean = cursorIndex > 0
    fun canNavigateNext(hasItems: Boolean): Boolean = hasItems
    fun pageDisplayNumber(cursorIndex: Int): Int = (cursorIndex + 1).coerceAtLeast(1)
}
