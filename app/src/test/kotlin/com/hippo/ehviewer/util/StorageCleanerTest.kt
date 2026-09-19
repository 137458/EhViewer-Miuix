package com.hippo.ehviewer.util

import com.ehviewer.core.database.model.DownloadEntity
import com.ehviewer.core.database.model.DownloadInfo
import com.ehviewer.core.database.model.GalleryEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StorageCleanerTest {

    private fun createDownloadInfo(gid: Long, dirname: String?, state: Int = DownloadInfo.STATE_FINISH): DownloadInfo {
        val gallery = GalleryEntity(
            gid = gid,
            token = "token_$gid",
            title = "Title $gid",
            titleJpn = null,
            thumbKey = null,
            category = 0,
            posted = null,
            uploader = null,
            rating = 0f,
            simpleTags = null,
            pages = 1,
            simpleLanguage = null,
            favoriteSlot = -1,
        )
        val entity = DownloadEntity(gid = gid, state = state)
        return DownloadInfo(galleryInfo = gallery, dirname = dirname, downloadInfo = entity)
    }

    @Test
    fun isDeadDownload_whenDirnameIsNull_returnsTrue() {
        val info = createDownloadInfo(1L, null)
        assertTrue(StorageCleaner.isDeadDownload(info))
    }

    @Test
    fun isDeadDownload_whenDirnameIsBlank_returnsTrue() {
        val info = createDownloadInfo(2L, "   ")
        assertTrue(StorageCleaner.isDeadDownload(info))
    }

    @Test
    fun isDeadDownload_whenDirectoryExists_returnsFalse() {
        val info = createDownloadInfo(3L, "gallery_3")
        assertFalse(StorageCleaner.isDeadDownload(info, dirExists = { true }, archiveExists = { false }))
    }

    @Test
    fun isDeadDownload_whenArchiveExists_returnsFalse() {
        val info = createDownloadInfo(4L, "gallery_4")
        assertFalse(StorageCleaner.isDeadDownload(info, dirExists = { false }, archiveExists = { true }))
    }

    @Test
    fun isDeadDownload_whenNeitherDirNorArchiveExists_returnsTrue() {
        val info = createDownloadInfo(5L, "gallery_5")
        assertTrue(StorageCleaner.isDeadDownload(info, dirExists = { false }, archiveExists = { false }))
    }

    @Test
    fun filterDeadDownloads_filtersAccurately_withMixedList() {
        val deadNull = createDownloadInfo(10L, null)
        val aliveDir = createDownloadInfo(11L, "gallery_11")
        val aliveArchive = createDownloadInfo(12L, "gallery_12")
        val deadMissing = createDownloadInfo(13L, "gallery_13")

        val list = listOf(deadNull, aliveDir, aliveArchive, deadMissing)
        val result = StorageCleaner.filterDeadDownloads(
            list,
            dirExists = { it.gid == 11L },
            archiveExists = { it.gid == 12L },
        )

        assertEquals(2, result.size)
        assertEquals(listOf(10L, 13L), result.map { it.gid })
    }
}
