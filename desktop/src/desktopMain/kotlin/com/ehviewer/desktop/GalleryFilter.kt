package com.ehviewer.desktop

import com.ehviewer.core.database.client.GalleryDetailUrlParser
import com.ehviewer.core.database.client.getCategoryName
import com.ehviewer.core.model.BaseGalleryInfo

object GalleryFilter {
    fun filterGalleries(
        items: List<BaseGalleryInfo>,
        query: String,
    ): List<BaseGalleryInfo> {
        val trimmed = query.trim()

        val matched = if (trimmed.isEmpty()) {
            items
        } else {
            val parsedUrl = GalleryDetailUrlParser.parse(trimmed, strict = false)
            if (parsedUrl != null) {
                items.filter { it.gid == parsedUrl.gid }
            } else {
                items.filter { item ->
                    item.title?.contains(trimmed, ignoreCase = true) == true ||
                        item.titleJpn?.contains(trimmed, ignoreCase = true) == true ||
                        item.uploader?.contains(trimmed, ignoreCase = true) == true ||
                        item.gid.toString().contains(trimmed) ||
                        item.simpleTags?.any { it.contains(trimmed, ignoreCase = true) } == true ||
                        getCategoryName(item.category).contains(trimmed, ignoreCase = true) ||
                        DesktopCategories.displayName(item.category).contains(trimmed, ignoreCase = true)
                }
            }
        }

        // 结果以 gid 作为 Compose LazyList 条目 key，源头按 gid 去重防 "Key was duplicated" 崩溃
        return matched.distinctBy { it.gid }
    }
}
