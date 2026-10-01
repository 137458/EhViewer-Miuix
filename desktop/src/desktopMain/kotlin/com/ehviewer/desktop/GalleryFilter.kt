package com.ehviewer.desktop

import com.ehviewer.core.model.BaseGalleryInfo

object GalleryFilter {
    fun filterGalleries(
        items: List<BaseGalleryInfo>,
        query: String,
    ): List<BaseGalleryInfo> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return items
        return items.filter { item ->
            item.title?.contains(trimmed, ignoreCase = true) == true ||
                item.titleJpn?.contains(trimmed, ignoreCase = true) == true ||
                item.uploader?.contains(trimmed, ignoreCase = true) == true ||
                item.gid.toString().contains(trimmed) ||
                item.simpleTags?.any { it.contains(trimmed, ignoreCase = true) } == true
        }
    }
}
