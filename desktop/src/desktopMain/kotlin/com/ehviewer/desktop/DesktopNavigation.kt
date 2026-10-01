package com.ehviewer.desktop

import com.ehviewer.core.model.BaseGalleryInfo

object DesktopNavigation {
    fun nextSelection(
        items: List<BaseGalleryInfo>,
        currentSelected: BaseGalleryInfo?,
    ): BaseGalleryInfo? {
        if (items.isEmpty()) return null
        if (currentSelected == null) return items.first()
        val currentIndex = items.indexOfFirst { it.gid == currentSelected.gid }
        if (currentIndex < 0) return items.first()
        val nextIndex = (currentIndex + 1).coerceAtMost(items.lastIndex)
        return items[nextIndex]
    }

    fun previousSelection(
        items: List<BaseGalleryInfo>,
        currentSelected: BaseGalleryInfo?,
    ): BaseGalleryInfo? {
        if (items.isEmpty()) return null
        if (currentSelected == null) return items.last()
        val currentIndex = items.indexOfFirst { it.gid == currentSelected.gid }
        if (currentIndex < 0) return items.last()
        val prevIndex = (currentIndex - 1).coerceAtLeast(0)
        return items[prevIndex]
    }
}
