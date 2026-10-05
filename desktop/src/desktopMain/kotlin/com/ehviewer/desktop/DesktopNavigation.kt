package com.ehviewer.desktop

import com.ehviewer.core.i18n.MR
import com.ehviewer.core.model.BaseGalleryInfo
import dev.icerock.moko.resources.StringResource

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

enum class LibraryTab {
    Home,
    Subscription,
    Whatshot,
    Toplist,
    Favorites,
    History,
    Downloads,
    ;

    companion object {
        fun fromName(raw: String?): LibraryTab = when (raw) {
            "Home" -> Home
            "Subscription" -> Subscription
            "Whatshot" -> Whatshot
            "Toplist" -> Toplist
            "Favorites" -> Favorites
            "History" -> History
            "Downloads" -> Downloads
            "Online" -> Home
            else -> History
        }
    }
}

val LibraryTab.isOnline: Boolean
    get() = when (this) {
        LibraryTab.Home,
        LibraryTab.Subscription,
        LibraryTab.Whatshot,
        LibraryTab.Toplist -> true
        else -> false
    }

val LibraryTab.titleRes: StringResource
    get() = when (this) {
        LibraryTab.Home -> MR.strings.homepage
        LibraryTab.Subscription -> MR.strings.subscription
        LibraryTab.Whatshot -> MR.strings.whats_hot
        LibraryTab.Toplist -> MR.strings.toplist
        LibraryTab.Favorites -> MR.strings.local_favorites
        LibraryTab.History -> MR.strings.history
        LibraryTab.Downloads -> MR.strings.download
    }

