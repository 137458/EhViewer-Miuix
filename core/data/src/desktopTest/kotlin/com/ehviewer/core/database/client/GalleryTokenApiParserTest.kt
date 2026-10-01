package com.ehviewer.core.database.client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// 桌面侧 Gallery Token API 解析下沉验证（tokenlist → 首个 token）
class GalleryTokenApiParserTest {

    @Test
    fun parseExtractsFirstToken() {
        val body = """
            {"tokenlist":[
              {"gid":618395,"token":"0439fa3666"},
              {"gid":618396,"token":"aaaabbbb"}
            ]}
        """.trimIndent()
        assertEquals("0439fa3666", GalleryTokenApiParser.parse(body))
    }

    @Test
    fun parseEmptyTokenListReturnsNull() {
        assertNull(GalleryTokenApiParser.parse("""{"tokenlist":[]}"""))
    }

    @Test
    fun parseMalformedReturnsNull() {
        assertNull(GalleryTokenApiParser.parse("not json"))
        assertNull(GalleryTokenApiParser.parse("""{"wrong":"shape"}"""))
        assertNull(GalleryTokenApiParser.parse(""))
    }
}
