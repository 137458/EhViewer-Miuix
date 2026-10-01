package com.ehviewer.desktop

import com.ehviewer.core.model.BaseGalleryInfo

object DesktopHistoryState {
    fun updateSelectionAfterDelete(
        currentSelected: BaseGalleryInfo?,
        deletedGid: Long,
    ): BaseGalleryInfo? = if (currentSelected?.gid == deletedGid) null else currentSelected

    fun updateSelectionAfterClearAll(
        currentSelected: BaseGalleryInfo?,
        currentTabIsHistory: Boolean,
    ): BaseGalleryInfo? = if (currentTabIsHistory) null else currentSelected
}
