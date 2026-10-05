package com.ehviewer.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    fun buildWithCursorAppendsNextParameter() {
        // 第一页（无游标）不带参数，与既有行为兼容
        assertEquals(
            "https://e-hentai.org/?f_search=touhou",
            DesktopSearchUrl.build("touhou"),
        )
        // 站点分页是游标式：next = 当前结果最后一项的 gid
        assertEquals(
            "https://e-hentai.org/?f_search=touhou&next=4221532",
            DesktopSearchUrl.build("touhou", nextGid = 4221532L),
        )
        assertEquals(
            "https://e-hentai.org/?f_search=%E6%9D%B1%E6%96%B9&next=42",
            DesktopSearchUrl.build("東方", nextGid = 42L),
        )
        // 负值视为无游标
        assertEquals(
            "https://e-hentai.org/?f_search=touhou",
            DesktopSearchUrl.build("touhou", nextGid = -1L),
        )
    }

    @Test
    fun buildWithPrevGidPrependsPrevParameter() {
        // 站点游标双向：prev={gid} 为从该 gid 起往前翻（往更早画廊方向）
        assertEquals(
            "https://e-hentai.org/?f_search=touhou&prev=4221532",
            DesktopSearchUrl.build("touhou", nextGid = 4221532L, backward = true),
        )
        assertEquals(
            "https://e-hentai.org/?f_search=%E6%9D%B1%E6%96%B9&prev=42",
            DesktopSearchUrl.build("東方", nextGid = 42L, backward = true),
        )
        // 负值视为无游标
        assertEquals(
            "https://e-hentai.org/?f_search=touhou",
            DesktopSearchUrl.build("touhou", nextGid = -1L, backward = true),
        )
    }

    @Test
    fun onlinePaginationBoundaryAndPageNumber() {
        // 第一页（cursorIndex = 0）无上一页
        assertFalse(DesktopOnlinePagination.canNavigatePrev(0))
        assertFalse(DesktopOnlinePagination.canNavigatePrev(-1))
        // 后续页有上一页
        assertTrue(DesktopOnlinePagination.canNavigatePrev(1))
        assertTrue(DesktopOnlinePagination.canNavigatePrev(5))

        // 页码展示计算
        assertEquals(1, DesktopOnlinePagination.pageDisplayNumber(0))
        assertEquals(2, DesktopOnlinePagination.pageDisplayNumber(1))
        assertEquals(1, DesktopOnlinePagination.pageDisplayNumber(-3))
    }
}
