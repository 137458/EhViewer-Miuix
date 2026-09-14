package com.hippo.ehviewer.download

import com.ehviewer.core.database.model.DownloadLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DownloadManagerTest {

    @Test
    fun removeLabelAndShiftPositions_whenLabelNotFound_returnsNullAndDoesNotThrow() {
        val list = mutableListOf(
            DownloadLabel("A", 0),
            DownloadLabel("B", 1),
        )

        val (removed, shifted) = removeLabelAndShiftPositions(list, "NonExistent")

        assertNull(removed)
        assertEquals(0, shifted.size)
        assertEquals(2, list.size)
        assertEquals(0, list[0].position)
        assertEquals(1, list[1].position)
    }

    @Test
    fun removeLabelAndShiftPositions_whenLabelFound_removesAndDecrementsSubsequentPositions() {
        val list = mutableListOf(
            DownloadLabel("A", 0),
            DownloadLabel("B", 1),
            DownloadLabel("C", 2),
            DownloadLabel("D", 3),
        )

        val (removed, shifted) = removeLabelAndShiftPositions(list, "B")

        assertEquals("B", removed?.label)
        assertEquals(2, shifted.size)
        assertEquals("C", shifted[0].label)
        assertEquals(1, shifted[0].position)
        assertEquals("D", shifted[1].label)
        assertEquals(2, shifted[1].position)

        assertEquals(3, list.size)
        assertEquals("A", list[0].label)
        assertEquals(0, list[0].position)
        assertEquals("C", list[1].label)
        assertEquals(1, list[1].position)
        assertEquals("D", list[2].label)
        assertEquals(2, list[2].position)
    }
}
