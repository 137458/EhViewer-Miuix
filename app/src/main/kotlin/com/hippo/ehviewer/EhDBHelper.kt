package com.hippo.ehviewer

fun shouldDeleteGallery(
    inDownloads: Boolean,
    inLocalFavorites: Boolean,
    inHistory: Boolean,
): Boolean {
    return !inDownloads && !inLocalFavorites && !inHistory
}
