package com.ehviewer.desktop

import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopHttpTest {
    @Test
    fun toByteBufferReturnsFlippedBufferForSuccess() {
        val buffer = DesktopResponse(200, "hello").toByteBuffer()
        assertEquals(5, buffer!!.remaining())
        val bytes = ByteArray(5)
        buffer.get(bytes)
        assertEquals("hello", String(bytes))
    }

    @Test
    fun toByteBufferNullForNonSuccessStatus() {
        assertNull(DesktopResponse(404, "gone").toByteBuffer())
        assertNull(DesktopResponse(500, "boom").toByteBuffer())
    }
}
