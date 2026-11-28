package com.hippo.ehviewer.ui.tools

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.cbor.Cbor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DateStoreStateTest {
    private val serializer = Int.serializer()

    @Test
    fun `decodes stored value`() {
        val bytes = Cbor.encodeToByteArray(serializer, 7)
        assertEquals(7, decodeStoredValue(serializer, bytes))
    }

    @Test
    fun `returns null for corrupted payload instead of throwing`() {
        assertNull(decodeStoredValue(serializer, byteArrayOf(0x7f, 0x00, 0x13, 0x37)))
    }
}
