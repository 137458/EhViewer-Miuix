package com.ehviewer.core.database.client

// 画廊详情页图片页链接提取（桌面阅读链路：详情页 HTML → 各页 /s/ 地址与页码）。
// 页码升序返回；base 取自首条匹配以兼容镜像域。
object GalleryDetailPageLinksParser {
    private val PATTERN_PAGE_LINK = Regex("href=\"([^\"]*/s/([0-9a-f]{10})/(\\d+)-(\\d+))(?:/)?\"")

    data class PageLink(
        val page: Int,
        val pToken: String,
        val pageUrl: String,
    )

    fun parse(body: String): List<PageLink> = PATTERN_PAGE_LINK.findAll(body)
        .map { match ->
            val (base, token, gid, page) = match.destructured
            PageLink(page = page.toInt(), pToken = token, pageUrl = base)
        }
        .distinctBy { it.page }
        .sortedBy { it.page }
        .toList()
}
