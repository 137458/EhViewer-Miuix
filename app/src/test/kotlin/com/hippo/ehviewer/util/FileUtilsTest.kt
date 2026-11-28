package com.hippo.ehviewer.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileUtilsTest {
    @Test
    fun `sanitizeFilename falls back for blank input`() {
        assertEquals("_", FileUtils.sanitizeFilename(""))
        assertEquals("_", FileUtils.sanitizeFilename("   "))
        assertEquals("_", FileUtils.sanitizeFilename(" ... "))
    }

    @Test
    fun `sanitizeFilename keeps surrogate pairs when truncating`() {
        val name = "a" + "😀".repeat(120)
        val sanitized = FileUtils.sanitizeFilename(name)

        assertTrue(sanitized.toByteArray().size <= 200, "truncated name exceeds the byte budget")
        assertEquals(
            sanitized.count { it.isHighSurrogate() },
            sanitized.count { it.isLowSurrogate() },
            "truncation split a surrogate pair",
        )
    }
}
