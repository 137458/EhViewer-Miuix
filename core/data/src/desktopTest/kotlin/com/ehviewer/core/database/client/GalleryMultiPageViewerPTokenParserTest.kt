package com.ehviewer.core.database.client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// 桌面侧 MPV pToken 解析下沉验证（MPV 页 imagelist JSON → token 列表）
class GalleryMultiPageViewerPTokenParserTest {

    @Test
    fun parseImagelistExtractsTokensInOrder() {
        val body = """
            <html><script>
            var imagelist = [{"k":"af19"},{"k":"b2c3"},{"k":"c4d5"}];
            </script></html>
        """.trimIndent()
        assertEquals(listOf("af19", "b2c3", "c4d5"), GalleryMultiPageViewerPTokenParser.parse(body))
    }

    @Test
    fun parseSingleItemAndEmptyList() {
        assertEquals(listOf("x9y8"), GalleryMultiPageViewerPTokenParser.parse("""var imagelist = [{"k":"x9y8"}];"""))
        assertEquals(emptyList(), GalleryMultiPageViewerPTokenParser.parse("""var imagelist = [];"""))
    }

    // 数组结尾按括号配对识别：没有尾分号、或字符串值里含分号时也要能解析
    @Test
    fun parseHandlesMissingSemicolonAndSemicolonInsideString() {
        assertEquals(listOf("af19"), GalleryMultiPageViewerPTokenParser.parse("""var imagelist = [{"k":"af19"}]"""))
        assertEquals(listOf("af19"), GalleryMultiPageViewerPTokenParser.parse("""var imagelist = [{"k":"af19","x":"a;b"}];"""))
    }

    @Test
    fun parseMalformedOrMissingReturnsNull() {
        assertNull(GalleryMultiPageViewerPTokenParser.parse("<html>no imagelist here</html>"))
        assertNull(GalleryMultiPageViewerPTokenParser.parse("var imagelist = [broken];"))
        assertNull(GalleryMultiPageViewerPTokenParser.parse(""))
        assertNull(GalleryMultiPageViewerPTokenParser.parse(null))
    }
}
