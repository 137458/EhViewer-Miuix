package com.hippo.ehviewer.util

import androidx.room.execSQL
import androidx.room.useWriterConnection
import com.ehviewer.core.database.model.DownloadInfo
import com.ehviewer.core.files.deleteContent
import com.ehviewer.core.files.exists
import com.ehviewer.core.util.mapToLongArray
import com.ehviewer.core.util.withIOContext
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.download.archiveFile
import com.hippo.ehviewer.download.downloadDir

data class CleanResult(
    val deadDownloadsCount: Int,
    val vacuumSuccess: Boolean,
)

object StorageCleaner {

    fun isDeadDownload(
        info: DownloadInfo,
        dirExists: (DownloadInfo) -> Boolean = { it.downloadDir?.exists() == true },
        archiveExists: (DownloadInfo) -> Boolean = { it.archiveFile?.exists() == true },
    ): Boolean {
        if (info.dirname.isNullOrBlank()) return true
        val hasDir = runCatching { dirExists(info) }.getOrDefault(false)
        val hasArchive = runCatching { archiveExists(info) }.getOrDefault(false)
        return !hasDir && !hasArchive
    }

    fun filterDeadDownloads(
        downloads: List<DownloadInfo>,
        dirExists: (DownloadInfo) -> Boolean = { it.downloadDir?.exists() == true },
        archiveExists: (DownloadInfo) -> Boolean = { it.archiveFile?.exists() == true },
    ): List<DownloadInfo> = downloads.filter { isDeadDownload(it, dirExists, archiveExists) }

    suspend fun cleanCache() = withIOContext {
        runCatching { EhApplication.thumbCache.clear() }
        runCatching { AppConfig.tempDir.deleteContent() }
        runCatching { AppConfig.externalTempDir?.deleteContent() }
        runCatching { AppConfig.externalTempPersistDir?.deleteContent() }
        runCatching { FileUtils.cleanupDirectory(AppConfig.externalCrashDir) }
        runCatching { FileUtils.cleanupDirectory(AppConfig.externalParseErrorDir) }
    }

    suspend fun cleanDeadDownloads(): Int = withIOContext {
        val allDownloads = DownloadManager.downloadInfoList.toList()
        val dead = filterDeadDownloads(allDownloads)
        if (dead.isNotEmpty()) {
            val gids = dead.mapToLongArray { it.gid }
            DownloadManager.deleteRangeDownload(gids)
            dead.forEach { info ->
                runCatching { EhDB.removeDownloadDirname(info.gid) }
            }
        }
        dead.size
    }

    suspend fun vacuumDatabases(): Boolean = withIOContext {
        val r1 = runCatching { EhDB.vacuumDB() }.isSuccess
        val r2 = runCatching {
            EhApplication.searchDatabase.useWriterConnection { conn ->
                conn.execSQL("PRAGMA wal_checkpoint(FULL)")
                conn.execSQL("VACUUM")
            }
        }.isSuccess
        r1 || r2
    }

    suspend fun performDeepClean(): CleanResult = withIOContext {
        cleanCache()
        val deadCount = cleanDeadDownloads()
        val vacuumOk = vacuumDatabases()
        CleanResult(deadDownloadsCount = deadCount, vacuumSuccess = vacuumOk)
    }
}
