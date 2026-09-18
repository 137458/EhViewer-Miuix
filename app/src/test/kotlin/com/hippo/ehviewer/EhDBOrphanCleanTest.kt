package com.hippo.ehviewer

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EhDBOrphanCleanTest {

    @Test
    fun shouldDeleteGallery_whenReferencedByDownloads_returnsFalse() {
        assertFalse(
            shouldDeleteGallery(
                inDownloads = true,
                inLocalFavorites = false,
                inHistory = false,
            ),
        )
    }

    @Test
    fun shouldDeleteGallery_whenReferencedByLocalFavorites_returnsFalse() {
        assertFalse(
            shouldDeleteGallery(
                inDownloads = false,
                inLocalFavorites = true,
                inHistory = false,
            ),
        )
    }

    @Test
    fun shouldDeleteGallery_whenReferencedByHistory_returnsFalse() {
        assertFalse(
            shouldDeleteGallery(
                inDownloads = false,
                inLocalFavorites = false,
                inHistory = true,
            ),
        )
    }

    @Test
    fun shouldDeleteGallery_whenReferencedByMultiple_returnsFalse() {
        assertFalse(
            shouldDeleteGallery(
                inDownloads = true,
                inLocalFavorites = true,
                inHistory = false,
            ),
        )
        assertFalse(
            shouldDeleteGallery(
                inDownloads = true,
                inLocalFavorites = true,
                inHistory = true,
            ),
        )
    }

    @Test
    fun shouldDeleteGallery_whenNoReferencesExist_returnsTrue() {
        assertTrue(
            shouldDeleteGallery(
                inDownloads = false,
                inLocalFavorites = false,
                inHistory = false,
            ),
        )
    }
}
