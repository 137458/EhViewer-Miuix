package com.hippo.ehviewer.gallery

import com.hippo.ehviewer.spider.shouldApplyTokenDelay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PreDecodePipelineTest {

    @Test
    fun `calculatePreDecodeRange properly limits window size and total pages boundary`() {
        // Normal case: at index 0 with window 2 out of 10 pages -> pages 1 and 2
        assertEquals(listOf(1, 2), calculatePreDecodeRange(currentIndex = 0, windowSize = 2, totalPages = 10))

        // Near end: at index 8 with window 2 out of 10 pages -> only page 9
        assertEquals(listOf(9), calculatePreDecodeRange(currentIndex = 8, windowSize = 2, totalPages = 10))

        // At last page: at index 9 out of 10 pages -> empty
        assertEquals(emptyList(), calculatePreDecodeRange(currentIndex = 9, windowSize = 2, totalPages = 10))

        // Zero window size -> empty
        assertEquals(emptyList(), calculatePreDecodeRange(currentIndex = 2, windowSize = 0, totalPages = 10))

        // Negative or boundary current index -> empty or clamped safely
        assertEquals(emptyList(), calculatePreDecodeRange(currentIndex = -1, windowSize = 2, totalPages = 10))
    }

    @Test
    fun `shouldApplyTokenDelay bypasses delay only for cached token in read mode`() {
        // When token is already cached in memory during online reading: ZERO delay (immediate non-blocking pipeline)
        assertFalse(
            shouldApplyTokenDelay(isTokenCached = true, isDownloadMode = false),
            "Online reading with cached token should bypass request delay",
        )

        // When token is not cached (requires network fetching): must apply delay to prevent rate limit
        assertTrue(
            shouldApplyTokenDelay(isTokenCached = false, isDownloadMode = false),
            "Uncached token requires delay for rate protection",
        )

        // When in batch download mode: always apply delay regardless of token cache to avoid server throttling
        assertTrue(
            shouldApplyTokenDelay(isTokenCached = true, isDownloadMode = true),
            "Download mode must always apply delay even if token cached",
        )
        assertTrue(
            shouldApplyTokenDelay(isTokenCached = false, isDownloadMode = true),
            "Download mode must always apply delay",
        )
    }

    @Test
    fun `isWithinPreDecodeWindow correctly identifies viewport neighbor pages`() {
        // Current index 0, window 2 -> indices 1 and 2 are in window
        assertTrue(isWithinPreDecodeWindow(pageIndex = 1, currentIndex = 0, windowSize = 2))
        assertTrue(isWithinPreDecodeWindow(pageIndex = 2, currentIndex = 0, windowSize = 2))

        // Current page itself is not in "pre-decode" window
        assertFalse(isWithinPreDecodeWindow(pageIndex = 0, currentIndex = 0, windowSize = 2))

        // Pages outside window
        assertFalse(isWithinPreDecodeWindow(pageIndex = 3, currentIndex = 0, windowSize = 2))
        assertFalse(isWithinPreDecodeWindow(pageIndex = 5, currentIndex = 0, windowSize = 2))

        // Negative page index
        assertFalse(isWithinPreDecodeWindow(pageIndex = -1, currentIndex = 0, windowSize = 2))
    }

    @Test
    fun `isWithinPreDecodeWindow handles reading progression and window shift`() {
        // When user is reading page 5 with window 2: targets are 6 and 7
        assertTrue(isWithinPreDecodeWindow(pageIndex = 6, currentIndex = 5, windowSize = 2))
        assertTrue(isWithinPreDecodeWindow(pageIndex = 7, currentIndex = 5, windowSize = 2))
        assertFalse(isWithinPreDecodeWindow(pageIndex = 5, currentIndex = 5, windowSize = 2))
        assertFalse(isWithinPreDecodeWindow(pageIndex = 8, currentIndex = 5, windowSize = 2))
        assertFalse(isWithinPreDecodeWindow(pageIndex = 4, currentIndex = 5, windowSize = 2))
    }
}
