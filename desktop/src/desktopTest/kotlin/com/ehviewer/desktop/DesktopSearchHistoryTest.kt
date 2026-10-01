package com.ehviewer.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopSearchHistoryTest {

    @Test
    fun addQuery_addsNewQueryToFront() {
        val initial = listOf("artist:alpha", "female:sole")
        val result = DesktopSearchHistory.addQuery(initial, "language:japanese")
        assertEquals(listOf("language:japanese", "artist:alpha", "female:sole"), result)
    }

    @Test
    fun addQuery_blankQuery_doesNotChangeList() {
        val initial = listOf("artist:alpha", "female:sole")
        val result1 = DesktopSearchHistory.addQuery(initial, "")
        val result2 = DesktopSearchHistory.addQuery(initial, "   ")
        assertEquals(initial, result1)
        assertEquals(initial, result2)
    }

    @Test
    fun addQuery_caseInsensitiveDuplicate_movesToFrontWithNewCasing() {
        val initial = listOf("artist:alpha", "female:sole", "misc:color")
        val result = DesktopSearchHistory.addQuery(initial, "Female:Sole")
        assertEquals(listOf("Female:Sole", "artist:alpha", "misc:color"), result)
    }

    @Test
    fun addQuery_sequentialQueries_updatesOrderAccurately() {
        var list = emptyList<String>()
        list = DesktopSearchHistory.addQuery(list, "QueryA")
        list = DesktopSearchHistory.addQuery(list, "QueryB")
        list = DesktopSearchHistory.addQuery(list, "querya") // duplicate with different case
        list = DesktopSearchHistory.addQuery(list, "QueryC")
        assertEquals(listOf("QueryC", "querya", "QueryB"), list)
    }

    @Test
    fun addQuery_truncatesWhenExceedsMaxItems() {
        val initial = (1..5).map { "tag$it" }
        val result = DesktopSearchHistory.addQuery(initial, "tagNew", maxItems = 4)
        assertEquals(listOf("tagNew", "tag1", "tag2", "tag3"), result)
    }

    @Test
    fun addQuery_zeroOrNegativeMaxItems_returnsEmpty() {
        val initial = listOf("a", "b")
        val result = DesktopSearchHistory.addQuery(initial, "c", maxItems = 0)
        assertEquals(emptyList(), result)
    }

    @Test
    fun removeQuery_removesExistingCaseInsensitively() {
        val initial = listOf("TagA", "TagB", "TagC")
        val result = DesktopSearchHistory.removeQuery(initial, "taga")
        assertEquals(listOf("TagB", "TagC"), result)
    }

    @Test
    fun removeQuery_nonExistentOrBlank_preservesList() {
        val initial = listOf("TagA", "TagB")
        val result1 = DesktopSearchHistory.removeQuery(initial, "TagZ")
        val result2 = DesktopSearchHistory.removeQuery(initial, "   ")
        assertEquals(initial, result1)
        assertEquals(initial, result2)
    }

    @Test
    fun removeQuery_andEncode_roundTrip() {
        val initial = listOf("artist:alpha", "female:sole", "character:alice", "misc:color")
        val removed = DesktopSearchHistory.removeQuery(initial, "FEMALE:sole")
        val encoded = DesktopSearchHistory.encode(removed)
        val decoded = DesktopSearchHistory.decode(encoded)
        assertEquals(listOf("artist:alpha", "character:alice", "misc:color"), decoded)
    }

    @Test
    fun clearAll_returnsEmptyList() {
        val initial = listOf("a", "b", "c")
        assertEquals(emptyList(), DesktopSearchHistory.clearAll())
    }

    @Test
    fun encodeAndDecode_roundTrip() {
        val items = listOf("artist:test", "character:alice", "language:chinese", "tag:山田 太郎")
        val encoded = DesktopSearchHistory.encode(items)
        val decoded = DesktopSearchHistory.decode(encoded)
        assertEquals(items, decoded)
    }

    @Test
    fun decode_handlesNullOrEmptyOrBlankAndFiltersEmptyLines() {
        assertEquals(emptyList(), DesktopSearchHistory.decode(null))
        assertEquals(emptyList(), DesktopSearchHistory.decode(""))
        assertEquals(emptyList(), DesktopSearchHistory.decode("   \n\n   \n"))
        val raw = "  tag1  \n\n tag2\n   \ntag1\n"
        assertEquals(listOf("tag1", "tag2"), DesktopSearchHistory.decode(raw))
    }

    @Test
    fun filterSuggestions_emptyQuery_returnsRecentHistory() {
        val history = listOf("tag1", "tag2", "tag3", "tag4", "tag5", "tag6")
        val suggestions = DesktopSearchHistory.filterSuggestions(history, "", maxSuggestions = 3)
        assertEquals(listOf("tag1", "tag2", "tag3"), suggestions)
    }

    @Test
    fun filterSuggestions_matchingQuery_returnsContainingItemsExcludingExact() {
        val history = listOf("artist:alpha", "artist:beta", "artist:alphabet", "cosplay", "artist:al")
        val suggestions = DesktopSearchHistory.filterSuggestions(history, "ARTIST:al")
        assertEquals(listOf("artist:alpha", "artist:alphabet"), suggestions)
    }

    @Test
    fun addQuery_withNewlines_sanitizesIntoSingleLineWithoutFragmentation() {
        val initial = listOf("artist:alpha")
        val result = DesktopSearchHistory.addQuery(initial, "tag:one\r\ntag:two\ntag:three")
        assertEquals(listOf("tag:one tag:two tag:three", "artist:alpha"), result)
        val roundTrip = DesktopSearchHistory.decode(DesktopSearchHistory.encode(result))
        assertEquals(listOf("tag:one tag:two tag:three", "artist:alpha"), roundTrip)
    }
}
