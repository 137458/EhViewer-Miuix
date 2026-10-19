package com.ehviewer.desktop

import com.ehviewer.core.database.client.CATEGORY_MANGA
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
    fun buildAppliesCategoryExclusionMask() {
        // f_cats 是排除位掩码：勾选 Manga(0x4) 时 = 全类掩码去 manga = 0x3FB
        assertEquals(
            "https://e-hentai.org/?f_cats=1019&f_search=touhou",
            DesktopSearchUrl.build("touhou", filters = DesktopSearchFilters(category = CATEGORY_MANGA)),
        )
        // 全选/默认不过滤
        assertEquals(
            "https://e-hentai.org/?f_search=touhou",
            DesktopSearchUrl.build(
                "touhou",
                filters = DesktopSearchFilters(category = DesktopSearchFilters.CATEGORY_ALL),
            ),
        )
        assertEquals(
            "https://e-hentai.org/?f_search=touhou",
            DesktopSearchUrl.build("touhou", filters = DesktopSearchFilters(category = 0)),
        )
    }

    @Test
    fun buildPrefixesLanguageTagIntoSearch() {
        assertEquals(
            "https://e-hentai.org/?f_search=language%3Achinese+touhou",
            DesktopSearchUrl.build("touhou", filters = DesktopSearchFilters(language = 1)),
        )
        // keyword 已带 language: 时不再注入
        assertEquals(
            "https://e-hentai.org/?f_search=language%3Ajapanese+touhou",
            DesktopSearchUrl.build("language:japanese touhou", filters = DesktopSearchFilters(language = 1)),
        )
        // 非法语言下标按无语言处理
        assertEquals(
            "https://e-hentai.org/?f_search=touhou",
            DesktopSearchUrl.build("touhou", filters = DesktopSearchFilters(language = 99)),
        )
        assertEquals(
            "https://e-hentai.org/?f_search=touhou",
            DesktopSearchUrl.build("touhou", filters = DesktopSearchFilters(language = -1)),
        )
    }

    @Test
    fun buildAppendsAdvancedSearchParams() {
        val filters = DesktopSearchFilters(
            advanceSearch = DesktopAdvanceSearchBits.SH or DesktopAdvanceSearchBits.SFT,
            minRating = 3,
            pageFrom = 10,
            pageTo = 50,
        )
        assertEquals(
            "https://e-hentai.org/?f_search=q&advsearch=1&f_sh=on&f_sft=on&f_srdd=3&f_spf=10&f_spt=50",
            DesktopSearchUrl.build("q", filters = filters),
        )
    }

    @Test
    fun buildAppendsRangeParameterForShuffle() {
        assertEquals(
            "https://e-hentai.org/?f_search=touhou&range=37",
            DesktopSearchUrl.build("touhou", range = 37),
        )
        // range<=0 不携带
        assertEquals(
            "https://e-hentai.org/?f_search=touhou",
            DesktopSearchUrl.build("touhou", range = 0),
        )
    }

    @Test
    fun buildCombinesCategoryCursorAndRange() {
        assertEquals(
            "https://e-hentai.org/?f_cats=1019&f_search=touhou&next=42&range=5",
            DesktopSearchUrl.build(
                "touhou",
                nextGid = 42L,
                range = 5,
                filters = DesktopSearchFilters(category = CATEGORY_MANGA),
            ),
        )
    }

    @Test
    fun buildBlankQueryWithActiveFiltersStillReturnsUrl() {
        // 空查询 + 分类过滤 = 站点分类浏览（无 f_search 段）
        assertEquals(
            "https://e-hentai.org/?f_cats=1019",
            DesktopSearchUrl.build("", filters = DesktopSearchFilters(category = CATEGORY_MANGA)),
        )
        // 空查询 + 纯高级开关 = 高级搜索入口
        assertEquals(
            "https://e-hentai.org/?advsearch=1&f_sh=on",
            DesktopSearchUrl.build("", filters = DesktopSearchFilters(advanceSearch = DesktopAdvanceSearchBits.SH)),
        )
    }

    @Test
    fun buildForTabDelegatesFiltersForHomeAndSubscription() {
        val filters = DesktopSearchFilters(category = CATEGORY_MANGA)
        assertEquals(
            "https://e-hentai.org/?f_cats=1019&f_search=touhou",
            DesktopSearchUrl.buildForTab(LibraryTab.Home, "touhou", filters = filters),
        )
        assertEquals(
            "https://e-hentai.org/watched?f_cats=1019&f_search=touhou",
            DesktopSearchUrl.buildForTab(LibraryTab.Subscription, "touhou", filters = filters),
        )
        // 空查询无过滤时维持旧默认
        assertEquals("https://e-hentai.org/", DesktopSearchUrl.buildForTab(LibraryTab.Home))
        assertEquals("https://e-hentai.org/watched", DesktopSearchUrl.buildForTab(LibraryTab.Subscription))
    }

    @Test
    fun buildForTabToplistSupportsPageJump() {
        assertEquals("https://e-hentai.org/toplist.php?tl=11", DesktopSearchUrl.buildForTab(LibraryTab.Toplist))
        // 排行榜末页：p = page - 1（与移动端 page setter 同语义）
        assertEquals(
            "https://e-hentai.org/toplist.php?tl=11&p=199",
            DesktopSearchUrl.buildForTab(LibraryTab.Toplist, page = 200),
        )
        // Whatshot 固定热门页，不受过滤影响
        assertEquals(
            "https://e-hentai.org/popular",
            DesktopSearchUrl.buildForTab(LibraryTab.Whatshot, "touhou", filters = DesktopSearchFilters(category = CATEGORY_MANGA)),
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
