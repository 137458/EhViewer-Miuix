package com.ehviewer.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopSearchFiltersTest {

    @Test
    fun decodeBlankAndGarbageYieldDefaults() {
        assertEquals(DesktopSearchFilters(), DesktopSearchFiltersCodec.decode(""))
        assertEquals(DesktopSearchFilters(), DesktopSearchFiltersCodec.decode("   "))
        assertEquals(DesktopSearchFilters(), DesktopSearchFiltersCodec.decode("not json at all"))
    }

    @Test
    fun encodeDecodeRoundTripPreservesFields() {
        val filters = DesktopSearchFilters(
            category = DesktopSearchFilters().category xor 0x2,
            language = 3,
            advanceSearch = DesktopAdvanceSearchBits.SH or DesktopAdvanceSearchBits.SFL,
            minRating = 4,
            pageFrom = 10,
            pageTo = 200,
        )
        val decoded = DesktopSearchFiltersCodec.decode(DesktopSearchFiltersCodec.encode(filters))
        assertEquals(filters, decoded)
    }

    @Test
    fun decodeIgnoresUnknownKeys() {
        val decoded = DesktopSearchFiltersCodec.decode("""{"category":16,"futureField":1}""")
        assertEquals(16, decoded.category)
        assertEquals(DesktopSearchFilters().language, decoded.language)
    }
}
