package com.ehviewer.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopReaderProgressTest {

    @Test
    fun saveAndRestoreWithinBounds() {
        DesktopReaderProgress.save(100L, 3)
        assertEquals(3, DesktopReaderProgress.restore(100L, totalPages = 10))
    }

    @Test
    fun restoreClampsToTotalPages() {
        // 画廊页数变少（换源/重解析）时钳制到末页，避免越界空白
        DesktopReaderProgress.save(200L, 15)
        assertEquals(10, DesktopReaderProgress.restore(200L, totalPages = 10))
    }

    @Test
    fun restoreDefaultsToFirstPageWhenAbsent() {
        assertEquals(1, DesktopReaderProgress.restore(300L, totalPages = 10))
    }

    @Test
    fun restoreHandlesZeroOrNegativeTotalPages() {
        DesktopReaderProgress.save(400L, 5)
        assertEquals(1, DesktopReaderProgress.restore(400L, totalPages = 0))
    }

    @Test
    fun saveClampsNonPositivePage() {
        DesktopReaderProgress.save(500L, 0)
        assertEquals(1, DesktopReaderProgress.restore(500L, totalPages = 10))
    }
}
