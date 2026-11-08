package com.hippo.ehviewer.legacy

import com.hippo.ehviewer.spider.SpiderInfo
import kotlinx.io.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpiderInfoCompatTest {

    private fun parse(content: String): SpiderInfo? = runCatching {
        val bytes = content.toByteArray(Charsets.UTF_8)
        parseLegacySpiderInfo(Buffer().also { it.write(bytes, 0, bytes.size) })
    }.getOrNull()

    @Test
    fun parsesVersion2File() {
        val info = parse(
            """
            VERSION2
            00000000
            12345
            token-abc
            1
            0
            -1
            42
            0 ptoken-0
            1 ptoken-1
            """.trimIndent() + "\n",
        )
        assertNotNull(info)
        assertEquals(12345L, info!!.gid)
        assertEquals("token-abc", info.token)
        assertEquals(42, info.pages)
        assertEquals(-1, info.previewPerPage)
        assertEquals("ptoken-0", info.pTokenMap[0])
        assertEquals("ptoken-1", info.pTokenMap[1])
    }

    @Test
    fun parsesVersion1FileWithoutPreviewPerPageLine() {
        val info = parse(
            """
            00000000
            12345
            token-abc
            1
            0
            42
            0 ptoken-0
            """.trimIndent() + "\n",
        )
        assertNotNull(info)
        assertEquals(12345L, info!!.gid)
        assertEquals("token-abc", info.token)
        assertEquals(42, info.pages)
        assertEquals(-1, info.previewPerPage)
        assertEquals("ptoken-0", info.pTokenMap[0])
    }

    @Test
    fun skipsCorruptPTokenLinesButKeepsValidOnes() {
        val info = parse(
            """
            VERSION2
            00000000
            12345
            token-abc
            1
            0
            -1
            42
            abc not-a-number
            1 valid-token
            """.trimIndent() + "\n",
        )
        assertNotNull(info)
        assertEquals("valid-token", info!!.pTokenMap[1])
        assertEquals(1, info.pTokenMap.size)
    }

    @Test
    fun rejectsPagesBeyondUpperBound() {
        val info = parse(
            """
            VERSION2
            00000000
            12345
            token-abc
            1
            0
            -1
            100001
            """.trimIndent() + "\n",
        )
        assertNull(info)
    }

    @Test
    fun skipsPTokenLongerThanCap() {
        val info = parse(
            """
            VERSION2
            00000000
            12345
            token-abc
            1
            0
            -1
            42
            0 ${"x".repeat(1100)}
            1 kept
            """.trimIndent() + "\n",
        )
        assertNotNull(info)
        assertTrue(info!!.pTokenMap.containsKey(1))
        assertEquals(1, info.pTokenMap.size)
    }

    @Test
    fun returnsNullForTruncatedHeader() {
        val info = parse("VERSION2\n00000000\n")
        assertNull(info)
    }

    @Test
    fun rejectsUnknownVersion() {
        val info = parse(
            """
            VERSION3
            00000000
            12345
            token-abc
            1
            0
            -1
            42
            """.trimIndent() + "\n",
        )
        assertNull(info)
    }
}
