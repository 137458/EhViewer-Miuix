package com.ehviewer.core.database.client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// 桌面阅读链路第一步：从画廊详情页 HTML 提取全部图片页链接（/s/{token}/{gid}-{page}/）
class GalleryDetailPageLinksParserTest {

    @Test
    fun parseExtractsPageLinksSortedByPage() {
        val body = """
            <div class="gdtl">
            <a href="https://e-hentai.org/s/def4567890/123456-3/">3</a>
            <a href="https://e-hentai.org/s/abc1234567/123456-1/">1</a>
            <a href="https://e-hentai.org/s/bbb2223333/123456-2/">2</a>
            </div>
        """.trimIndent()
        val links = GalleryDetailPageLinksParser.parse(body)
        assertEquals(3, links.size)
        // 按页码排序；真实链接无尾斜杠（base 即完整地址）
        assertEquals(listOf(1, 2, 3), links.map { it.page })
        assertEquals("abc1234567", links[0].pToken)
        assertEquals("def4567890", links[2].pToken)
        assertEquals("https://e-hentai.org/s/abc1234567/123456-1", links[0].pageUrl)
    }

    @Test
    fun parseIgnoresNonPageLinks() {
        val body = """
            <a href="https://e-hentai.org/g/123456/abcdef1234/">gallery</a>
            <a href="https://e-hentai.org/s/def4567890/123456-1/">1</a>
            <a href="https://forums.e-hentai.org/">forums</a>
        """.trimIndent()
        val links = GalleryDetailPageLinksParser.parse(body)
        assertEquals(1, links.size)
        assertEquals(1, links[0].page)
    }

    @Test
    fun parseEmptyOrNoLinksReturnsEmpty() {
        assertTrue(GalleryDetailPageLinksParser.parse("").isEmpty())
        assertTrue(GalleryDetailPageLinksParser.parse("<html></html>").isEmpty())
    }
}
