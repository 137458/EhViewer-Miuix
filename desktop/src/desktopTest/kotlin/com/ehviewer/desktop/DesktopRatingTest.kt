package com.ehviewer.desktop

import com.ehviewer.core.database.client.CATEGORY_MANGA
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
        val meta = DesktopRating.formatCardMeta(pages = 25, category = CATEGORY_MANGA)
        assertEquals("25P · Manga", meta)

        val metaZeroPages = DesktopRating.formatCardMeta(pages = 0, category = CATEGORY_MANGA)
        assertEquals("Manga", metaZeroPages)

        val metaNegativePages = DesktopRating.formatCardMeta(pages = -5, category = CATEGORY_MANGA)
        assertEquals("Manga", metaNegativePages)
    }
}
