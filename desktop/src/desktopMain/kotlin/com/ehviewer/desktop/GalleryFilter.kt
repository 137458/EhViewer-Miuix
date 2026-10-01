package com.ehviewer.desktop

import com.ehviewer.core.database.client.GalleryDetailUrlParser
import com.ehviewer.core.model.BaseGalleryInfo

object GalleryFilter {
    fun filterGalleries(
        items: List<BaseGalleryInfo>,
        query: String,
    ): List<BaseGalleryInfo> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return items

        val parsedUrl = GalleryDetailUrlParser.parse(trimmed, strict = false)
        if (parsedUrl != null) {
            return items.filter { it.gid == parsedUrl.gid }
        }

        return items.filter { item ->
            item.title?.contains(trimmed, ignoreCase = true) == true ||
                item.titleJpn?.contains(trimmed, ignoreCase = true) == true ||
                item.uploader?.contains(trimmed, ignoreCase = true) == true ||
                item.gid.toString().contains(trimmed) ||
                item.simpleTags?.any { it.contains(trimmed, ignoreCase = true) } == true
        }
    }
}
