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
        // posted 固定按 UTC 格式化为 yyyy-MM-dd HH:mm（与 Android ParserUtils.formatDate 语义一致）
        assertEquals("2023-11-14 22:13", gi1.posted)
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
        // 与 EhUtils 对齐：Private 别名映射 0x400，不得落入 UNKNOWN
        assertEquals(CATEGORY_PRIVATE, getCategory("private"))
        assertEquals(CATEGORY_UNKNOWN, getCategory("nonexistent"))
        assertEquals(CATEGORY_UNKNOWN, getCategory(null))
    }

    @Test
    fun categoryNameAndDisplayNameMapping() {
        assertEquals("doujinshi", getCategoryName(CATEGORY_DOUJINSHI))
        assertEquals("Doujinshi", getCategoryDisplayName(CATEGORY_DOUJINSHI))
        assertEquals("manga", getCategoryName(CATEGORY_MANGA))
        assertEquals("Manga", getCategoryDisplayName(CATEGORY_MANGA))
        assertEquals("artistcg", getCategoryName(CATEGORY_ARTIST_CG))
        assertEquals("Artist CG", getCategoryDisplayName(CATEGORY_ARTIST_CG))
        assertEquals("gamecg", getCategoryName(CATEGORY_GAME_CG))
        assertEquals("Game CG", getCategoryDisplayName(CATEGORY_GAME_CG))
        assertEquals("imageset", getCategoryName(CATEGORY_IMAGE_SET))
        assertEquals("Image Set", getCategoryDisplayName(CATEGORY_IMAGE_SET))
        assertEquals("cosplay", getCategoryName(CATEGORY_COSPLAY))
        assertEquals("Cosplay", getCategoryDisplayName(CATEGORY_COSPLAY))
        assertEquals("asianporn", getCategoryName(CATEGORY_ASIAN_PORN))
        assertEquals("Asian Porn", getCategoryDisplayName(CATEGORY_ASIAN_PORN))
        assertEquals("non-h", getCategoryName(CATEGORY_NON_H))
        assertEquals("Non-H", getCategoryDisplayName(CATEGORY_NON_H))
        assertEquals("western", getCategoryName(CATEGORY_WESTERN))
        assertEquals("Western", getCategoryDisplayName(CATEGORY_WESTERN))
        assertEquals("misc", getCategoryName(CATEGORY_MISC))
        assertEquals("Misc", getCategoryDisplayName(CATEGORY_MISC))
        assertEquals("private", getCategoryName(CATEGORY_PRIVATE))
        assertEquals("Private", getCategoryDisplayName(CATEGORY_PRIVATE))
        assertEquals("unknown", getCategoryName(CATEGORY_UNKNOWN))
        assertEquals("Unknown", getCategoryDisplayName(CATEGORY_UNKNOWN))
        assertEquals("unknown", getCategoryName(0))
        assertEquals("Unknown", getCategoryDisplayName(0))
        assertEquals("unknown", getCategoryName(-1))
        assertEquals("Unknown", getCategoryDisplayName(-1))
    }

    @Test
    fun thumbKeyStripsAllKnownPrefixes() {
        assertEquals("a/b.jpg", getThumbKey("https://ehgt.org/a/b.jpg"))
        // 前缀链依次剥离：exhentai 的 t/ 变体在剥离主域后被一并剥除（与 app 实现一致）
        assertEquals("x.jpg", getThumbKey("https://s.exhentai.org/t/x.jpg"))
        assertEquals("x.jpg", getThumbKey("https://s.exhentai.org/x.jpg"))
        assertEquals("plain", getThumbKey("plain"))
    }

    @Test
    fun keyToThumbUrlReconstructsCoverUrl() {
        // e-hentai (jpg 走 ehgt.org)
        assertEquals("https://ehgt.org/abc/def.jpg", keyToThumbUrl("abc/def.jpg", isExHentai = false))
        // exhentai (jpg 走 s.exhentai.org/t/)
        assertEquals("https://s.exhentai.org/t/abc/def.jpg", keyToThumbUrl("abc/def.jpg", isExHentai = true))
        // webp 格式：e-hentai 走 ehgt.org，exhentai 走 s.exhentai.org（无 /t/）
        assertEquals("https://ehgt.org/abc/def.webp", keyToThumbUrl("abc/def.webp", isExHentai = false))
        assertEquals("https://s.exhentai.org/abc/def.webp", keyToThumbUrl("abc/def.webp", isExHentai = true))
        // 已经是完整绝对 URL 保持不变
        assertEquals("https://example.com/custom.png", keyToThumbUrl("https://example.com/custom.png"))

        // GalleryInfo.thumbUrl 扩展属性
        val info = BaseGalleryInfo(thumbKey = "test/cover.jpg")
        assertEquals("https://ehgt.org/test/cover.jpg", info.thumbUrl)
        val nullInfo = BaseGalleryInfo(thumbKey = null)
        assertNull(nullInfo.thumbUrl)
    }
}
