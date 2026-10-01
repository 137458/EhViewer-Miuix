package com.ehviewer.core.database.client

import com.ehviewer.core.model.BaseGalleryInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// gdata JSON 响应解析的 fixture 驱动验证：字段映射/未知字段容错/分类映射/日期格式
class GalleryMetadataParserTest {
    @Test
    fun parseFillsMatchingGalleryInfoAndToleratesUnknownKeys() {
        val gi1 = BaseGalleryInfo(gid = 1, token = "tok1")
        val gi2 = BaseGalleryInfo(gid = 2, token = "tok2")
        val giNoMatch = BaseGalleryInfo(gid = 99, token = "none")

        val body = """
            {
              "gmetadata": [
                {
                  "gid": 1,
                  "token": "tok1",
                  "title": "Title &amp; Notes [Group]",
                  "title_jpn": "日本語タイトル",
                  "category": "Manga",
                  "thumb": "https://ehgt.org/abc/def.jpg",
                  "uploader": "someone",
                  "posted": 1700000000,
                  "filecount": 20,
                  "rating": 4.5,
                  "tags": ["language:chinese", "artist:foo"],
                  "unknown_future_field": 123
                },
                {
                  "gid": 2,
                  "token": "tok2",
                  "title": "Second",
                  "title_jpn": "",
                  "category": "western",
                  "thumb": "https://s.exhentai.org/t/xyz.jpg",
                  "uploader": null,
                  "posted": 1690000000,
                  "filecount": 5,
                  "rating": 3.0,
                  "tags": []
                }
              ]
            }
        """.trimIndent()

        GalleryMetadataParser.parse(body, listOf(gi1, gi2, giNoMatch))

        assertEquals("Title & Notes [Group]", gi1.title)
        assertEquals("日本語タイトル", gi1.titleJpn)
        assertEquals(CATEGORY_MANGA, gi1.category)
        assertEquals("abc/def.jpg", gi1.thumbKey)
        assertEquals("someone", gi1.uploader)
        assertEquals(20, gi1.pages)
        assertEquals(4.5f, gi1.rating)
        assertEquals(listOf("language:chinese", "artist:foo"), gi1.simpleTags)
        // posted 格式化为 yyyy-MM-dd HH:mm（UTC 由调用环境时区决定，仅验证形状）
        assertTrue(Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}""").matches(gi1.posted.orEmpty()))
        // generateSLang 的 language 映射：chinese -> ZH
        assertEquals("ZH", gi1.simpleLanguage)

        assertEquals(CATEGORY_WESTERN, gi2.category)
        assertEquals("xyz.jpg", gi2.thumbKey)
        assertNull(gi2.uploader)
        assertEquals(0, gi2.simpleTags?.size)

        // gid 不在响应中的列表项不被触碰
        assertNull(giNoMatch.title)
    }

    @Test
    fun categoryMappingCoversCaseVariantsAndFallsBackToUnknown() {
        assertEquals(CATEGORY_DOUJINSHI, getCategory("Doujinshi"))
        assertEquals(CATEGORY_ARTIST_CG, getCategory("Artist CG Sets"))
        assertEquals(CATEGORY_GAME_CG, getCategory("GAMECG"))
        assertEquals(CATEGORY_UNKNOWN, getCategory("nonexistent"))
        assertEquals(CATEGORY_UNKNOWN, getCategory(null))
    }

    @Test
    fun thumbKeyStripsAllKnownPrefixes() {
        assertEquals("a/b.jpg", getThumbKey("https://ehgt.org/a/b.jpg"))
        // 前缀链依次剥离：exhentai 的 t/ 变体在剥离主域后被一并剥除（与 app 实现一致）
        assertEquals("x.jpg", getThumbKey("https://s.exhentai.org/t/x.jpg"))
        assertEquals("x.jpg", getThumbKey("https://s.exhentai.org/x.jpg"))
        assertEquals("plain", getThumbKey("plain"))
    }
}
