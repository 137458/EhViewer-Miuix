package com.ehviewer.core.database.client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

// 桌面侧 GalleryPageParser 下沉验证（图片页 HTML → 图片地址/跳转键/原图/showkey）
class GalleryPageParserTest {

    @Test
    fun parseStandardPageExtractsAllFields() {
        val body = """
            <div id="i1">
            <img id="img" src="https://s.exhentai.org/h/001abc/123456/kw.jpg" style="visibility:hidden">
            <a href="https://exhentai.org/fullimg/98765/11223344/">Download original</a>
            <p onclick="return nl('98765-11223344')">click</p>
            <script>var showkey="abc123def0";</script>
            </div>
        """.trimIndent()
        val result = GalleryPageParser.parse(body)
        assertNotNull(result)
        assertEquals("https://s.exhentai.org/h/001abc/123456/kw.jpg", result.imageUrl)
        assertEquals("98765-11223344", result.skipHathKey)
        assertEquals("https://exhentai.org/fullimg/98765/11223344/", result.originImageUrl)
        assertEquals("abc123def0", result.showKey)
    }

    @Test
    fun parseUnescapesXmlEntitiesInImageUrl() {
        val body = """<img src="https://s.exhentai.org/h/a/1/k&amp;w=2.jpg" style>"""
        val result = GalleryPageParser.parse(body)
        assertNotNull(result)
        assertEquals("https://s.exhentai.org/h/a/1/k&w=2.jpg", result.imageUrl)
    }

    @Test
    fun parseOptionalFieldsMayBeAbsent() {
        val body = """<img src="https://ehgt.org/only/img.jpg" style>"""
        val result = GalleryPageParser.parse(body)
        assertNotNull(result)
        assertEquals("https://ehgt.org/only/img.jpg", result.imageUrl)
        assertNull(result.skipHathKey)
        assertNull(result.originImageUrl)
        assertNull(result.showKey)
    }

    @Test
    fun parsePageWithoutImageUrlReturnsNull() {
        assertNull(GalleryPageParser.parse("<html><body>ip banned</body></html>"))
        assertNull(GalleryPageParser.parse(""))
    }
}
