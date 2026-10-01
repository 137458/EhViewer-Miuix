package com.ehviewer.desktop

import com.ehviewer.core.database.model.GalleryEntity
import com.ehviewer.core.model.BaseGalleryInfo

object DesktopFavoritesState {
    fun isFavorite(favoriteGids: Set<Long>, gid: Long): Boolean = gid in favoriteGids

    fun toggleFavoriteGid(currentFavorites: Set<Long>, gid: Long): Set<Long> = if (gid in currentFavorites) {
        currentFavorites - gid
    } else {
        currentFavorites + gid
    }

    fun toggleFavorite(currentFavorites: Set<Long>, gid: Long): Pair<Set<Long>, Boolean> {
        val isFav = gid in currentFavorites
        val updated = if (isFav) currentFavorites - gid else currentFavorites + gid
        return Pair(updated, !isFav)
    }

    fun filterFavorites(
        allGalleries: List<BaseGalleryInfo>,
        favoriteGids: Set<Long>,
    ): List<BaseGalleryInfo> {
        if (allGalleries.isEmpty() || favoriteGids.isEmpty()) return emptyList()
        val seen = mutableSetOf<Long>()
        return allGalleries.filter { gallery ->
            gallery.gid in favoriteGids && seen.add(gallery.gid)
        }
    }

    fun updateSelectionAfterRemoveFavorite(
        selected: BaseGalleryInfo?,
        removedGid: Long,
        isInFavoritesTab: Boolean,
    ): BaseGalleryInfo? {
        if (selected == null) return null
        return if (isInFavoritesTab && selected.gid == removedGid) {
            null
        } else {
            selected
        }
    }

    fun toGalleryEntity(gallery: BaseGalleryInfo): GalleryEntity = if (gallery is GalleryEntity) {
        gallery
    } else {
        GalleryEntity(
            gid = gallery.gid,
            token = gallery.token,
            title = gallery.title,
            titleJpn = gallery.titleJpn,
            thumbKey = gallery.thumbKey,
            category = gallery.category,
            posted = gallery.posted,
            uploader = gallery.uploader,
            rating = gallery.rating,
            simpleTags = gallery.simpleTags,
            pages = gallery.pages,
            simpleLanguage = gallery.simpleLanguage,
            favoriteSlot = gallery.favoriteSlot,
        )
    }
}
