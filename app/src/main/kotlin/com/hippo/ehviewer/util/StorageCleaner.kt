package com.hippo.ehviewer.util

import androidx.room.execSQL
import androidx.room.useWriterConnection
import com.ehviewer.core.database.model.DownloadInfo
import com.ehviewer.core.files.deleteContent
import com.ehviewer.core.files.exists
import com.ehviewer.core.files.isDirectory
import com.ehviewer.core.files.list
import com.ehviewer.core.util.mapToLongArray
import com.ehviewer.core.util.withIOContext
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.download.archiveFile
import com.hippo.ehviewer.download.downloadDir
import com.hippo.ehviewer.download.downloadLocation
import moe.tarsin.coroutines.runSuspendCatching

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
        // 只有已完成的任务才可能是死链：等待/下载中的任务目录还没建好，误删会丢掉用户正在下的本子
        if (info.state != DownloadInfo.STATE_FINISH) return false
        // dirname 为空说明这条下载根本没有落盘目录（旧版本残留），直接算死链；
        // 不能交给路径探测——空白 dirname 在 Windows 上会被归一化成下载目录本身，从而被误判成"还在"
        if (info.dirname.isNullOrBlank()) return true
        // 探测抛异常（存储未挂载、权限丢失）说明无法判断，保守地当作任务还活着
        val hasDir = runCatching { dirExists(info) }.getOrDefault(true)
        val hasArchive = runCatching { archiveExists(info) }.getOrDefault(true)
        return !hasDir && !hasArchive
    }

    fun filterDeadDownloads(
        downloads: List<DownloadInfo>,
        dirExists: (DownloadInfo) -> Boolean = { it.downloadDir?.exists() == true },
        archiveExists: (DownloadInfo) -> Boolean = { it.archiveFile?.exists() == true },
    ): List<DownloadInfo> = downloads.filter { isDeadDownload(it, dirExists, archiveExists) }

    suspend fun cleanCache() = withIOContext {
        runSuspendCatching { EhApplication.thumbCache.clear() }
        runSuspendCatching { AppConfig.tempDir.deleteContent() }
        runSuspendCatching { AppConfig.externalTempDir?.deleteContent() }
        cleanTempPersistDir()
        runSuspendCatching { FileUtils.cleanupDirectory(AppConfig.externalCrashDir) }
        runSuspendCatching { FileUtils.cleanupDirectory(AppConfig.externalParseErrorDir) }
    }

    // externalTempPersistDir 里既有正在下载的任务残留，也有已经结束的垃圾，
    // 只能按 gid 判断后清理，不能整目录删
    private fun cleanTempPersistDir() {
        val root = AppConfig.externalTempPersistDir ?: return
        val children = runCatching { root.list() }.getOrNull() ?: return
        children.forEach { child ->
            val gid = child.name.toLongOrNull()
            if (gid != null && DownloadManager.getDownloadInfo(gid)?.state != DownloadInfo.STATE_FINISH) {
                return@forEach
            }
            runCatching { child.deleteContent() }
        }
    }

    suspend fun cleanDeadDownloads(): Int = withIOContext {
        // 下载目录本身不可用（未挂载/路径失效）时，所有任务都会被误判成死链，直接放弃本轮清理
        if (!runCatching { downloadLocation.isAbsolute && downloadLocation.isDirectory }.getOrDefault(false)) {
            return@withIOContext 0
        }
        val allDownloads = DownloadManager.downloadInfoList.toList()
        val dead = filterDeadDownloads(allDownloads)
        if (dead.isNotEmpty()) {
            val gids = dead.mapToLongArray { it.gid }
            DownloadManager.deleteRangeDownload(gids)
            dead.forEach { info ->
                runSuspendCatching { EhDB.removeDownloadDirname(info.gid) }
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
        // 两个数据库都必须整理成功才算成功，否则其中一个的失败会被另一个掩盖
        r1 && r2
    }

    suspend fun performDeepClean(): CleanResult = withIOContext {
        cleanCache()
        val deadCount = cleanDeadDownloads()
        val vacuumOk = vacuumDatabases()
        CleanResult(deadDownloadsCount = deadCount, vacuumSuccess = vacuumOk)
    }
}
