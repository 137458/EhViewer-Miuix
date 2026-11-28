package com.hippo.ehviewer.client

import kotlin.test.Test
import kotlin.test.assertEquals

class EhTitleUtilsTest {

    // Reference implementation: the regular expressions EhTitleUtils replaced.
    private val legacyPrefix = Regex("""^(?:\([^)]*\)|\[[^]]*]|\{[^}]*\}|~[^~]*~|\s+)*""")
    private val legacySuffix = Regex(
        """(?:\s+ch.[\s\d-]+)?(?:\([^)]*\)|\[[^]]*]|\{[^}]*\}|~[^~]*~|\s+)*$""",
        RegexOption.IGNORE_CASE,
    )

    private fun legacyStripDecorations(title: String): String = legacySuffix.replaceFirst(legacyPrefix.replaceFirst(title, ""), "")

    private val titles = listOf(
        "[Artist] Title ch. 12 (English)",
        "Title [DL版] [中国翻訳]",
        "(a)(b) ch.3-5",
        "Title ~x~ {y} 5",
        "Title (English) ch. 5",
        "  [A] [B] {C} ~D~ Actual Title (v2) [x]  ",
        "(同人誌) [サークル] タイトル",
        "Title",
        "",
        "   ",
        "(unclosed [skip] rest",
        "ch. 3",
        "Title|ch. 12",
        "Title ch. 12 ch. 13",
        "[A]Title[B]",
        "Title ~unpaired",
        "Title ((nested)) tail",
    )

    @Test
    fun stripDecorations_matchesLegacyRegex() {
        titles.forEach { title ->
            assertEquals(legacyStripDecorations(title), EhTitleUtils.stripDecorations(title), "title=[$title]")
        }
    }

    @Test
    fun stripLeading_removesOnlyLeadingDecorations() {
        assertEquals("Title ch. 12", EhTitleUtils.stripLeading("[Artist]  Title ch. 12"))
        assertEquals("Title", EhTitleUtils.stripLeading("Title"))
        assertEquals("", EhTitleUtils.stripLeading("(only) [decorations] "))
    }

    @Test
    fun stripDecorations_handlesLongWhitespaceWithoutStackOverflow() {
        val padded = "Title" + " ".repeat(20_000) + "!"
        assertEquals(padded, EhTitleUtils.stripDecorations(padded))
        val trailing = "Title" + " ".repeat(20_000) + "[DL版]" + " ".repeat(20_000)
        assertEquals("Title", EhTitleUtils.stripDecorations(trailing))
    }
}
