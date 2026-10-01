package com.ehviewer.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopSearchUrlTest {

    @Test
    fun buildEncodesSpacesAndCjk() {
        assertEquals(
            "https://e-hentai.org/?f_search=touhou+project",
            DesktopSearchUrl.build("touhou project"),
        )
        // CJK 走 UTF-8 百分号编码
        assertEquals(
            "https://e-hentai.org/?f_search=%E6%9D%B1%E6%96%B9",
            DesktopSearchUrl.build("東方"),
        )
    }

    @Test
    fun buildTrimsAndEncodesSpecialCharacters() {
        assertEquals(
            "https://e-hentai.org/?f_search=abc",
            DesktopSearchUrl.build("  abc  "),
        )
        // & 等保留字符需编码，避免破坏 query 结构
        assertEquals(
            "https://e-hentai.org/?f_search=a%26b%3Dc",
            DesktopSearchUrl.build("a&b=c"),
        )
    }

    @Test
    fun buildBlankQueryReturnsNull() {
        assertEquals(null, DesktopSearchUrl.build(""))
        assertEquals(null, DesktopSearchUrl.build("   "))
    }

    @Test
    fun buildWithPageAppendsPageParameter() {
        // 第一页（0）不带 page 参数，与既有行为兼容
        assertEquals(
            "https://e-hentai.org/?f_search=touhou",
            DesktopSearchUrl.build("touhou", page = 0),
        )
        assertEquals(
            "https://e-hentai.org/?f_search=touhou&page=1",
            DesktopSearchUrl.build("touhou", page = 1),
        )
        assertEquals(
            "https://e-hentai.org/?f_search=%E6%9D%B1%E6%96%B9&page=3",
            DesktopSearchUrl.build("東方", page = 3),
        )
        // 负页码视为第一页
        assertEquals(
            "https://e-hentai.org/?f_search=touhou",
            DesktopSearchUrl.build("touhou", page = -1),
        )
    }
}
