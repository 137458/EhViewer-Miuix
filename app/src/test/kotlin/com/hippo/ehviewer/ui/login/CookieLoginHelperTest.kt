package com.hippo.ehviewer.ui.login

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CookieLoginHelperTest {

    @Test
    fun parseStandardSemicolonCookieString() {
        val raw = "ipb_member_id=1234567; ipb_pass_hash=0123456789abcdef; igneous=random_igneous_val; star=1"
        val parsed = CookieLoginHelper.parseCookieString(raw)

        assertEquals("1234567", parsed.memberId)
        assertEquals("0123456789abcdef", parsed.passHash)
        assertEquals("random_igneous_val", parsed.igneous)
        assertTrue(parsed.isValid)
    }

    @Test
    fun parseNewlineOrColonDelimitedCookieString() {
        val raw = """
            ipb_member_id: 987654
            ipb_pass_hash: fedcba9876543210
            igneous: my_igneous
        """.trimIndent()
        val parsed = CookieLoginHelper.parseCookieString(raw)

        assertEquals("987654", parsed.memberId)
        assertEquals("fedcba9876543210", parsed.passHash)
        assertEquals("my_igneous", parsed.igneous)
        assertTrue(parsed.isValid)
    }

    @Test
    fun parseCookieWithExtraWhitespaceAndQuotes() {
        val raw = "  ipb_member_id = \"555666\" ;   ipb_pass_hash = \"abcdef\" ; igneous = \"mysterious\" "
        val parsed = CookieLoginHelper.parseCookieString(raw)

        assertEquals("555666", parsed.memberId)
        assertEquals("abcdef", parsed.passHash)
        assertEquals("mysterious", parsed.igneous)
        assertTrue(parsed.isValid)
    }

    @Test
    fun parseMissingRequiredFields() {
        val raw = "igneous=abc; star=1"
        val parsed = CookieLoginHelper.parseCookieString(raw)

        assertNull(parsed.memberId)
        assertNull(parsed.passHash)
        assertEquals("abc", parsed.igneous)
        assertFalse(parsed.isValid)
    }

    @Test
    fun validateAndFormatAcceptsValidFields() {
        val result = CookieLoginHelper.validateAndFormat(
            memberId = " 12345 ",
            passHash = " abcdef ",
            igneous = "  ig_val  ",
        )
        assertNotNull(result)
        assertEquals("12345", result.memberId)
        assertEquals("abcdef", result.passHash)
        assertEquals("ig_val", result.igneous)
        assertTrue(result.isValid)
    }

    @Test
    fun validateAndFormatRejectsEmptyRequiredFields() {
        val result1 = CookieLoginHelper.validateAndFormat(
            memberId = "   ",
            passHash = "abcdef",
            igneous = null,
        )
        assertNull(result1)

        val result2 = CookieLoginHelper.validateAndFormat(
            memberId = "12345",
            passHash = "",
            igneous = null,
        )
        assertNull(result2)
    }
}
