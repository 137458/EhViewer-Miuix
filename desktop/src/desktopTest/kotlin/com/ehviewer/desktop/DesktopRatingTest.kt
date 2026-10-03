package com.ehviewer.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopRatingTest {

    @Test
    fun formatRatingScoreHandlesStandardCases() {
        assertEquals("0.00", DesktopRating.formatRatingScore(0.0f))
        assertEquals("4.50", DesktopRating.formatRatingScore(4.5f))
        assertEquals("4.85", DesktopRating.formatRatingScore(4.85f))
        assertEquals("5.00", DesktopRating.formatRatingScore(5.0f))
        assertEquals("3.05", DesktopRating.formatRatingScore(3.05f))
    }

    @Test
    fun formatRatingScoreClampsOutOfBoundsValues() {
        assertEquals("0.00", DesktopRating.formatRatingScore(-1.5f))
        assertEquals("5.00", DesktopRating.formatRatingScore(6.2f))
    }

    @Test
    fun formatRatingScoreRoundsProperly() {
        assertEquals("4.86", DesktopRating.formatRatingScore(4.856f))
        assertEquals("4.85", DesktopRating.formatRatingScore(4.851f))
    }

    @Test
    fun formatCardMetaProducesReadableSummary() {
        // 类目显示名由调用方经 DesktopCategories 注入；此处用等价字符串断言纯逻辑
        val meta = DesktopRating.formatCardMeta(pages = 25, categoryName = "MANGA")
        assertEquals("25P · MANGA", meta)

        val metaZeroPages = DesktopRating.formatCardMeta(pages = 0, categoryName = "MANGA")
        assertEquals("MANGA", metaZeroPages)

        val metaNegativePages = DesktopRating.formatCardMeta(pages = -5, categoryName = "MANGA")
        assertEquals("MANGA", metaNegativePages)
    }
}
