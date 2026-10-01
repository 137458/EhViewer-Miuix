package com.ehviewer.desktop

import com.ehviewer.core.model.BaseGalleryInfo

enum class DesktopSortField {
    Default,
    Rating,
    Pages,
    Title,
    ;

    fun next(): DesktopSortField = when (this) {
        Default -> Rating
        Rating -> Pages
        Pages -> Title
        Title -> Default
    }
}

enum class DesktopSortDirection {
    Ascending,
    Descending,
    ;

    fun toggle(): DesktopSortDirection = when (this) {
        Ascending -> Descending
        Descending -> Ascending
    }
}

data class DesktopSortConfig(
    val field: DesktopSortField = DesktopSortField.Default,
    val direction: DesktopSortDirection = DesktopSortDirection.Descending,
) {
    fun cycle(): DesktopSortConfig = when {
        field == DesktopSortField.Default -> copy(field = DesktopSortField.Rating, direction = DesktopSortDirection.Descending)
        direction == DesktopSortDirection.Descending -> copy(direction = DesktopSortDirection.Ascending)
        else -> {
            val nextField = field.next()
            if (nextField == DesktopSortField.Default) {
                DesktopSortConfig(field = DesktopSortField.Default)
            } else {
                DesktopSortConfig(field = nextField, direction = DesktopSortDirection.Descending)
            }
        }
    }

    val label: String
        get() = when (this.field) {
            DesktopSortField.Default -> "Sort"
            DesktopSortField.Rating -> if (direction == DesktopSortDirection.Descending) "★↓" else "★↑"
            DesktopSortField.Pages -> if (direction == DesktopSortDirection.Descending) "P↓" else "P↑"
            DesktopSortField.Title -> if (direction == DesktopSortDirection.Descending) "A-Z↓" else "A-Z↑"
        }

    fun sort(galleries: List<BaseGalleryInfo>): List<BaseGalleryInfo> {
        if (galleries.size <= 1 || field == DesktopSortField.Default) {
            return galleries
        }

        val comparator = when (field) {
            DesktopSortField.Default -> return galleries
            DesktopSortField.Rating -> compareBy<BaseGalleryInfo> { it.rating }
            DesktopSortField.Pages -> compareBy<BaseGalleryInfo> { it.pages }
            DesktopSortField.Title -> compareBy<BaseGalleryInfo, String>(String.CASE_INSENSITIVE_ORDER) {
                it.title?.trim()?.ifEmpty { null } ?: it.gid.toString()
            }
        }

        return if (direction == DesktopSortDirection.Descending) {
            galleries.sortedWith(comparator.reversed())
        } else {
            galleries.sortedWith(comparator)
        }
    }
}
