package com.ehviewer.desktop

import com.ehviewer.core.model.BaseGalleryInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopHistoryStateTest {

    @Test
    fun updateSelectionAfterDelete_whenTargetIsSelected_clearsSelection() {
        val item1 = BaseGalleryInfo(gid = 101L, title = "Item 101")
        val result = DesktopHistoryState.updateSelectionAfterDelete(item1, 101L)
        assertNull(result)
    }

    @Test
    fun updateSelectionAfterDelete_whenOtherIsSelected_preservesSelection() {
        val item1 = BaseGalleryInfo(gid = 101L, title = "Item 101")
        val result = DesktopHistoryState.updateSelectionAfterDelete(item1, 102L)
        assertEquals(item1, result)
    }

    @Test
    fun updateSelectionAfterDelete_whenNoneSelected_returnsNull() {
        val result = DesktopHistoryState.updateSelectionAfterDelete(null, 101L)
        assertNull(result)
    }

    @Test
    fun updateSelectionAfterClearAll_whenInHistoryTab_clearsSelection() {
        val item1 = BaseGalleryInfo(gid = 101L, title = "Item 101")
        val result = DesktopHistoryState.updateSelectionAfterClearAll(
            currentSelected = item1,
            currentTabIsHistory = true,
        )
        assertNull(result)

        val item2 = BaseGalleryInfo(gid = 202L, title = "Item 202")
        val result2 = DesktopHistoryState.updateSelectionAfterClearAll(
            currentSelected = item2,
            currentTabIsHistory = true,
        )
        assertNull(result2)
    }

    @Test
    fun updateSelectionAfterClearAll_whenNotInHistoryTab_preservesSelection() {
        val item1 = BaseGalleryInfo(gid = 101L, title = "Item 101")
        val result = DesktopHistoryState.updateSelectionAfterClearAll(
            currentSelected = item1,
            currentTabIsHistory = false,
        )
        assertEquals(item1, result)

        val item2 = BaseGalleryInfo(gid = 202L, title = "Item 202")
        val result2 = DesktopHistoryState.updateSelectionAfterClearAll(
            currentSelected = item2,
            currentTabIsHistory = false,
        )
        assertEquals(item2, result2)
    }

    @Test
    fun updateSelectionAfterClearAll_whenNoneSelected_returnsNull() {
        val resultInHistory = DesktopHistoryState.updateSelectionAfterClearAll(
            currentSelected = null,
            currentTabIsHistory = true,
        )
        assertNull(resultInHistory)

        val resultNotInHistory = DesktopHistoryState.updateSelectionAfterClearAll(
            currentSelected = null,
            currentTabIsHistory = false,
        )
        assertNull(resultNotInHistory)
    }
}
