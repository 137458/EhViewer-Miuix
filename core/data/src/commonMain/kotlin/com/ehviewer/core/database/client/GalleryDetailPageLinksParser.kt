package com.ehviewer.core.database.client

// 画廊详情页图片页链接提取（桌面阅读链路：详情页 HTML → 各页 /s/ 地址与页码）。
// 页码升序返回；base 取自首条匹配以兼容镜像域。
object GalleryDetailPageLinksParser {
    private val PATTERN_PAGE_LINK = Regex("href=\"([^\"]*/s/([0-9a-f]{10})/(\\d+)-(\\d+))(?:/)?\"")

    private val TRUSTED_HOSTS = setOf("e-hentai.org", "exhentai.org")

    data class PageLink(
        val page: Int,
        val pToken: String,
        val pageUrl: String,
    )

    fun parse(body: String, expectedGid: Long? = null): List<PageLink> = PATTERN_PAGE_LINK.findAll(body)
        .mapNotNull { match ->
            val (base, token, gid, page) = match.destructured
            if (!isTrustedUrl(base)) return@mapNotNull null
            val pageNumber = page.toIntOrNull() ?: return@mapNotNull null
            if (pageNumber <= 0) return@mapNotNull null
            if (expectedGid != null && gid.toLongOrNull() != expectedGid) return@mapNotNull null
            PageLink(page = pageNumber, pToken = token, pageUrl = base)
        }
        .distinctBy { it.page }
        .sortedBy { it.page }
        .toList()

    // 只接受官方域（含子域）：主机名逐字比对，杜绝 evil.example/s/... 之类伪造链接进入阅读队列
    private fun isTrustedUrl(url: String): Boolean {
        val host = url.substringAfter("://", "")
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .substringBefore(':')
            .lowercase()
        return TRUSTED_HOSTS.any { host == it || host.endsWith(".$it") }
    }
}
