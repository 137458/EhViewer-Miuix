package com.hippo.ehviewer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EhDBFtsQueryTest {

    @Test
    fun buildFtsQuery_singleToken_becomesQuotedPrefixQuery() {
        assertEquals("\"artist\"*", buildFtsQuery("artist"))
    }

    @Test
    fun buildFtsQuery_multipleTokens_areJoinedById() {
        assertEquals("\"foo\"* \"bar\"*", buildFtsQuery("foo bar"))
        assertEquals("\"foo\"* \"bar\"*", buildFtsQuery("  foo   bar  "))
    }

    @Test
    fun buildFtsQuery_quotesAreDoubled_soOperatorsStayLiteral() {
        assertEquals("\"a\"\"b\"*", buildFtsQuery("a\"b"))
        assertEquals("\"**abc**\"*", buildFtsQuery("**abc**"))
    }

    @Test
    fun buildFtsQuery_keywordWithoutSearchableToken_returnsNull() {
        assertNull(buildFtsQuery(""))
        assertNull(buildFtsQuery("   "))
        assertNull(buildFtsQuery("\t\n"))
        assertNull(buildFtsQuery("*"))
        assertNull(buildFtsQuery("*** \"\" -"))
    }

    @Test
    fun buildFtsQuery_punctuationOnlyTokens_areDropped() {
        assertEquals("\"abc\"*", buildFtsQuery("*** abc -"))
        assertEquals("\"100%\"*", buildFtsQuery("100%"))
    }
}
