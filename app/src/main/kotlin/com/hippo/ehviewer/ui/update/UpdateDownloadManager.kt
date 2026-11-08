package com.hippo.ehviewer.ui.update

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ehviewer.core.files.delete
import com.ehviewer.core.i18n.R
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.download.downloadLocation
import com.hippo.ehviewer.updater.ApkDigest
import com.hippo.ehviewer.updater.AppUpdater
import com.hippo.ehviewer.updater.DigestCheck
import com.hippo.ehviewer.updater.Release
import com.hippo.ehviewer.updater.updateFailureMessage
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.FileUtils
import com.hippo.ehviewer.util.ReadableTime
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import splitties.init.appCtx

/**
 * 全局更新下载管理器。
 * 托管于独立持久协程作用域，彻底解决更新弹窗 Dismiss / 进入后台时下载任务被取消的问题。
 * 集中管理下载状态，消除 UI 层状态泥团 (Data Clumps)。
 *
 * 下载结果（文件 / 错误）始终携带 [releaseVersion]，弹窗只认当前 release 的结果，
 * 避免同一进程内切换版本后把旧安装包或旧错误当成当前版本的。
 */
object UpdateDownloadManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val generation = AtomicLong()
    private var downloadJob: Job? = null

    /** 当前（或最近一次）下载所属的 release 版本；无结果时为 null。 */
    var releaseVersion by mutableStateOf<String?>(null)
        private set
    var isDownloading by mutableStateOf(false)
        private set
    var downloadProgress by mutableFloatStateOf(0f)
        private set
    var downloadedBytes by mutableLongStateOf(0L)
        private set
    var totalBytes by mutableLongStateOf(0L)
        private set
    var downloadSpeed by mutableLongStateOf(0L)
        private set
    var downloadedFile by mutableStateOf<File?>(null)
        private set
    var downloadError by mutableStateOf<String?>(null)
        private set

    /**
     * 代际判定的唯一出口：任务被取消或被新任务取代后 generation 已经推进，
     * 旧协程回飞的进度、完成与失败回调一律作废。
     */
    private fun stale(gen: Long) = generation.get() != gen

    /** 把「判定 + 写状态」绑成一个动作，新增写入点时无需再手写守卫。 */
    private inline fun ifFresh(gen: Long, block: () -> Unit) {
        if (!stale(gen)) block()
    }

    /**
     * 推进代际并停止当前 Job。先自增再 cancel，可让取消期间仍在回飞的旧回调立刻失效。
     * 返回的新代际交给紧接着发起的新任务认领，避免「取消 + 发起」自增两次。
     */
    private fun stopCurrentTask(): Long {
        val gen = generation.incrementAndGet()
        downloadJob?.cancel()
        downloadJob = null
        return gen
    }

    fun cancel() {
        stopCurrentTask()
        isDownloading = false
        downloadProgress = 0f
        downloadedBytes = 0L
        totalBytes = 0L
        downloadSpeed = 0L
        downloadedFile = null
        downloadError = null
        releaseVersion = null
    }

    /** 仅当结果确实属于 [version] 时才返回可安装文件，版本不符视为不可安装。 */
    fun installableFile(version: String): File? =
        downloadedFile?.takeIf { releaseVersion == version && it.exists() }

    /**
     * 丢弃不属于 [version] 的旧结果：删除旧文件并清空错误，
     * 供弹窗切到新版本时调用，避免把旧版本安装包当成新版本可用。
     */
    fun discardStaleResult(version: String) {
        if (isDownloading || releaseVersion == version) return
        val staleFile = downloadedFile
        if (staleFile == null && downloadError == null && releaseVersion == null) return
        downloadedFile = null
        downloadError = null
        releaseVersion = null
        if (staleFile != null) scope.launch { staleFile.delete() }
    }

    fun startDownload(release: Release, context: Context = appCtx) {
        val downloadGeneration = stopCurrentTask()
        releaseVersion = release.version
        isDownloading = true
        downloadError = null
        downloadedFile = null
        downloadProgress = 0f
        downloadedBytes = 0L
        totalBytes = 0L
        downloadSpeed = 0L

        var lastSpeedUpdateTime = System.currentTimeMillis()
        var lastSpeedBytes = 0L

        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                if (Settings.backupBeforeUpdate.value) {
                    runCatching {
                        val time = ReadableTime.getFilenamableTime()
                        EhDB.exportDB(downloadLocation / "$time.db")
                    }
                }

                val targetPath = AppConfig.tempDir / "update.apk"
                targetPath.delete()
                AppUpdater.downloadUpdate(
                    url = release.downloadLink,
                    path = targetPath,
                    onProgress = { progress, downloaded, total ->
                        if (stale(downloadGeneration)) return@downloadUpdate
                        downloadProgress = progress
                        downloadedBytes = downloaded
                        totalBytes = total

                        val now = System.currentTimeMillis()
                        val dt = now - lastSpeedUpdateTime
                        if (dt >= 400L) {
                            val dBytes = downloaded - lastSpeedBytes
                            if (dBytes > 0L) {
                                val instantSpeed = (dBytes * 1000L) / dt
                                downloadSpeed = if (downloadSpeed == 0L) instantSpeed else (downloadSpeed * 7 + instantSpeed * 3) / 10
                            }
                            lastSpeedUpdateTime = now
                            lastSpeedBytes = downloaded
                        }
                    },
                )
                val file = targetPath.toFile()
                // CI 下载的是 zip 构件，回调里的总长是压缩包体积，解压后的 APK 不能按它核对。
                val isArchive = release.downloadLink.endsWith("zip")
                if (!isArchive && totalBytes > 0L && file.length() != totalBytes) {
                    val actualSize = file.length()
                    val expectedSize = totalBytes
                    ifFresh(downloadGeneration) {
                        // 先确认代际，再删文件：否则新一轮下载可能已写入同名文件，旧任务会把它删掉。
                        targetPath.delete()
                        fail(
                            downloadGeneration,
                            context,
                            context.getString(
                                R.string.update_error_incomplete,
                                FileUtils.humanReadableByteCount(actualSize),
                                FileUtils.humanReadableByteCount(expectedSize),
                            ),
                        )
                    }
                    return@launch
                }
                when (val digestCheck = ApkDigest.verify(file, release.apkDigest)) {
                    is DigestCheck.Mismatched -> {
                        ifFresh(downloadGeneration) {
                            targetPath.delete()
                            fail(
                                downloadGeneration,
                                context,
                                context.getString(
                                    R.string.update_error_digest,
                                    digestCheck.expected.take(12),
                                    digestCheck.actual.take(12),
                                ),
                            )
                        }
                        return@launch
                    }
                    DigestCheck.Skipped, DigestCheck.Verified -> Unit
                }
                ifFresh(downloadGeneration) {
                    downloadedFile = file
                    isDownloading = false
                    downloadJob = null
                    if (AppForegroundTracker.isForeground) {
                        installUpdatePackage(context, file)
                    } else {
                        UpdateNotifications.notifyDownloaded(context)
                    }
                }
            } catch (e: Exception) {
                ifFresh(downloadGeneration) {
                    isDownloading = false
                    downloadJob = null
                    if (e !is CancellationException) {
                        fail(downloadGeneration, context, updateFailureMessage(context, e))
                    }
                }
            }
        }
        downloadJob = job
        job.start()
    }

    private fun fail(gen: Long, context: Context, message: String) {
        ifFresh(gen) {
            isDownloading = false
            downloadJob = null
            downloadError = message
            if (!AppForegroundTracker.isForeground) {
                UpdateNotifications.notifyFailed(context, message)
            }
        }
    }
}
