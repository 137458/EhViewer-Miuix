package com.ehviewer.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopTagFormatterTest {

    @Test
    fun splitTags_normalAndCommaSeparated() {
        val input = listOf("artist:alpha", "female:sole, parody:touhou", "  female:sole  ", "cosplay")
        val result = DesktopTagFormatter.splitTags(input)
        assertEquals(listOf("artist:alpha", "female:sole", "parody:touhou", "cosplay"), result)
    }

    @Test
    fun splitTags_nullOrEmpty_returnsEmptyList() {
        assertEquals(emptyList(), DesktopTagFormatter.splitTags(null))
        assertEquals(emptyList(), DesktopTagFormatter.splitTags(emptyList()))
        assertEquals(emptyList(), DesktopTagFormatter.splitTags(listOf("  ", "", ",,")))
    }

    @Test
    fun parseTagNamespace_withColon() {
        val (ns, name) = DesktopTagFormatter.parseTagNamespace("artist:shindo l")
        assertEquals("artist", ns)
        assertEquals("shindo l", name)
    }

    @Test
    fun parseTagNamespace_withoutColon_defaultsToMisc() {
        val (ns, name) = DesktopTagFormatter.parseTagNamespace("cosplay")
        assertEquals("misc", ns)
        assertEquals("cosplay", name)
    }

    @Test
    fun parseTagNamespace_multipleColons_takesFirstAsNamespace() {
        val (ns, name) = DesktopTagFormatter.parseTagNamespace("parody:fate:grand order")
        assertEquals("parody", ns)
        assertEquals("fate:grand order", name)
    }

    @Test
    fun groupTags_groupsByNamespaceCorrectly() {
        val tags = listOf(
            "artist:alpha",
            "female:sole",
            "female:stockings",
            "parody:touhou",
            "cosplay",
        )
        val grouped = DesktopTagFormatter.groupTags(tags)
        assertEquals(4, grouped.size)
        assertEquals(listOf("alpha"), grouped["artist"])
        assertEquals(listOf("sole", "stockings"), grouped["female"])
        assertEquals(listOf("touhou"), grouped["parody"])
        assertEquals(listOf("cosplay"), grouped["misc"])
    }

    @Test
    fun groupTags_deduplicatesTagsWithinSameNamespace() {
        val tags = listOf("artist:alpha", "artist:alpha", "artist:beta")
        val grouped = DesktopTagFormatter.groupTags(tags)
        assertEquals(listOf("alpha", "beta"), grouped["artist"])
    }

    @Test
    fun groupTags_nullOrEmpty_returnsEmptyMap() {
        assertEquals(emptyMap(), DesktopTagFormatter.groupTags(null))
        assertEquals(emptyMap(), DesktopTagFormatter.groupTags(emptyList()))
    }

    @Test
    fun formatTagQuery_miscReturnsNameOnly_othersReturnColonPrefixed() {
        assertEquals("cosplay", DesktopTagFormatter.formatTagQuery("misc", "cosplay"))
        assertEquals("cosplay", DesktopTagFormatter.formatTagQuery(" Misc ", "cosplay"))
        assertEquals("artist:shindo l", DesktopTagFormatter.formatTagQuery("artist", "shindo l"))
        assertEquals("female:sole", DesktopTagFormatter.formatTagQuery("female", "sole"))
    }

    @Test
    fun generateShareSummary_formatsComprehensiveMetadata() {
        val summary = DesktopTagFormatter.generateShareSummary(
            title = "Test Gallery Title",
            gid = 123456L,
            token = "abcdef1234",
            rating = 4.85f,
            pages = 32,
            category = 2, // Doujinshi
            tags = listOf("artist:alpha", "female:sole"),
        )
        assertTrue(summary.contains("Test Gallery Title"))
        assertTrue(summary.contains("https://e-hentai.org/g/123456/abcdef1234/"))
        assertTrue(summary.contains("4.85"))
        assertTrue(summary.contains("32"))
        assertTrue(summary.contains("Doujinshi"))
        assertTrue(summary.contains("artist:alpha, female:sole"))
    }

    @Test
    fun generateShareSummary_whenNoTags_displaysNone() {
        val summary = DesktopTagFormatter.generateShareSummary(
            title = "Untitled",
            gid = 1L,
            token = "abc",
            rating = 0.0f,
            pages = 0,
            category = 0,
            tags = null,
        )
        assertTrue(summary.contains("Tags: None"))
    }
}
