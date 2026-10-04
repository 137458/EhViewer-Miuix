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

    @Test
    fun updateMovesEntryToFrontForLruEviction() {
        // LRU 语义：最近阅读的条目（哪怕原在中部）移到最前，decode 截断时淘汰的是最旧条目而非最新写入
        val progress = linkedMapOf(1L to 1, 2L to 2, 3L to 3)
        val updated = DesktopReadingProgress.update(progress, 2L, 5)
        assertEquals(2L, updated.keys.first())
        assertEquals(5, updated[2L])
    }

    @Test
    fun decodeKeepsNewestAndDropsOldest() {
        // 51 条且最新(1L)在最前、最旧(51L)在末尾：截断后应保留 1L、淘汰 51L
        val newestFirst = linkedMapOf(1L to 1)
        for (gid in 2L..51L) newestFirst[gid] = gid.toInt()
        val decoded = DesktopReadingProgress.decode(DesktopReadingProgress.encode(newestFirst))
        assertEquals(DesktopReadingProgress.MAX_ENTRIES, decoded.size)
        assertTrue(decoded.containsKey(1L))
        assertTrue(!decoded.containsKey(51L))
    }
}
