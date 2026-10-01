package com.ehviewer.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopReadingProgressTest {

    @Test
    fun serializeRoundTripsProgressMap() {
        val progress = mapOf(100L to 5, 200L to 12)
        val encoded = DesktopReadingProgress.encode(progress)
        assertEquals(progress, DesktopReadingProgress.decode(encoded))
    }

    @Test
    fun updateClampsPageAndReplacesEntry() {
        val progress = mapOf(100L to 5)
        // 正常前进
        val updated = DesktopReadingProgress.update(progress, 100L, 8)
        assertEquals(8, updated[100L])
        // 页码钳位到至少 1
        assertEquals(1, DesktopReadingProgress.update(progress, 100L, 0)[100L])
        assertEquals(1, DesktopReadingProgress.update(progress, 100L, -3)[100L])
        // 新画廊条目
        assertTrue(DesktopReadingProgress.update(progress, 200L, 1).containsKey(200L))
    }

    @Test
    fun decodeCorruptOrBlankReturnsEmpty() {
        assertTrue(DesktopReadingProgress.decode(null).isEmpty())
        assertTrue(DesktopReadingProgress.decode("").isEmpty())
        assertTrue(DesktopReadingProgress.decode("not json").isEmpty())
        assertTrue(DesktopReadingProgress.decode("""{"bad":"shape"}""").isEmpty())
    }

    @Test
    fun decodeCapsProgressEntries() {
        val many = (1L..60L).associateWith { it.toInt() }
        val decoded = DesktopReadingProgress.decode(DesktopReadingProgress.encode(many))
        assertEquals(DesktopReadingProgress.MAX_ENTRIES, decoded.size)
    }
}
