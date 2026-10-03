package com.hippo.ehviewer.ui.update

import android.content.Context
import android.widget.Toast
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
import com.hippo.ehviewer.updater.AppUpdater
import com.hippo.ehviewer.updater.Release
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.ReadableTime
import com.hippo.ehviewer.util.installPackage
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import splitties.init.appCtx

/**
 * 全局更新下载管理器。
 * 托管于独立持久协程作用域，彻底解决更新弹窗 Dismiss / 进入后台时下载任务被取消的问题。
 * 集中管理下载状态，消除 UI 层状态泥团 (Data Clumps)。
 */
object UpdateDownloadManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val generation = AtomicLong()
    private var downloadJob: Job? = null

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
     * 旧协程回飞的进度、模拟步进、完成与失败回调一律作废。
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
        downloadSpeed = 0L
    }

    fun startDownload(release: Release, isSimulated: Boolean, context: Context = appCtx) {
        val downloadGeneration = stopCurrentTask()
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
                if (isSimulated) {
                    // ── 交互式沙盒模拟链路 ──
                    val simTotal = if (release.apkSize > 0L) release.apkSize else 44256789L
                    if (stale(downloadGeneration)) return@launch
                    totalBytes = simTotal
                    val steps = 30
                    for (i in 1..steps) {
                        delay(80)
                        if (stale(downloadGeneration)) return@launch
                        val p = i.toFloat() / steps
                        downloadProgress = p
                        downloadedBytes = (simTotal * p).toLong()
                        downloadSpeed = (7_500_000L + (Math.random() * 2_500_000L).toLong())
                    }
                    ifFresh(downloadGeneration) {
                        isDownloading = false
                        downloadedFile = File(AppConfig.tempDir.toFile(), "ehviewer-update-simulated.apk")
                        downloadJob = null
                        Toast.makeText(context, context.getString(R.string.update_dialog_ready_install), Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

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
                ifFresh(downloadGeneration) {
                    downloadedFile = file
                    isDownloading = false
                    downloadJob = null
                    with(context) { runCatching { installPackage(file) } }
                }
            } catch (e: Exception) {
                ifFresh(downloadGeneration) {
                    isDownloading = false
                    downloadJob = null
                    if (e !is CancellationException) {
                        downloadError = e.localizedMessage ?: context.getString(R.string.update_download_failed)
                    }
                }
            }
        }
        downloadJob = job
        job.start()
    }
}
