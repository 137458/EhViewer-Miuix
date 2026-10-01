package com.ehviewer.desktop

import com.ehviewer.core.model.BaseGalleryInfo

object DesktopHistoryState {
    fun <T : BaseGalleryInfo> removeGallery(
        currentList: List<T>,
        targetGid: Long,
    ): List<T> = currentList.filterNot { it.gid == targetGid }

    fun updateSelectionAfterDelete(
        currentSelected: BaseGalleryInfo?,
        deletedGid: Long,
    ): BaseGalleryInfo? = if (currentSelected?.gid == deletedGid) null else currentSelected
}
