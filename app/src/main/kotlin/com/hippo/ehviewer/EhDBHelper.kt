package com.hippo.ehviewer

fun shouldDeleteGallery(
    inDownloads: Boolean,
    inLocalFavorites: Boolean,
    inHistory: Boolean,
): Boolean = !inDownloads && !inLocalFavorites && !inHistory
