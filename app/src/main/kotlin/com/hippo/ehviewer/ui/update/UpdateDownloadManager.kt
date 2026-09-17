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
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
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

    fun cancel() {
        downloadJob?.cancel()
        downloadJob = null
        isDownloading = false
        downloadProgress = 0f
        downloadedBytes = 0L
        downloadSpeed = 0L
    }

    fun startDownload(release: Release, isSimulated: Boolean, context: Context = appCtx) {
        cancel()
        isDownloading = true
        downloadError = null
        downloadedFile = null
        downloadProgress = 0f
        downloadedBytes = 0L
        totalBytes = 0L
        downloadSpeed = 0L

        var lastSpeedUpdateTime = System.currentTimeMillis()
        var lastSpeedBytes = 0L

        downloadJob = scope.launch {
            if (isSimulated) {
                // ── 交互式沙盒模拟链路 ──
                val simTotal = if (release.apkSize > 0L) release.apkSize else 44256789L
                totalBytes = simTotal
                val steps = 30
                for (i in 1..steps) {
                    delay(80)
                    val p = i.toFloat() / steps
                    downloadProgress = p
                    downloadedBytes = (simTotal * p).toLong()
                    downloadSpeed = (7_500_000L + (Math.random() * 2_500_000L).toLong())
                }
                isDownloading = false
                downloadedFile = File(AppConfig.tempDir.toFile(), "ehviewer-update-simulated.apk")
                downloadJob = null
                Toast.makeText(context, context.getString(R.string.update_dialog_ready_install), Toast.LENGTH_SHORT).show()
                return@launch
            }

            if (Settings.backupBeforeUpdate.value) {
                runCatching {
                    val time = ReadableTime.getFilenamableTime()
                    EhDB.exportDB(downloadLocation / "$time.db")
                }
            }

            val targetPath = AppConfig.tempDir / "update.apk"
            try {
                targetPath.delete()
                AppUpdater.downloadUpdate(
                    url = release.downloadLink,
                    path = targetPath,
                    onProgress = { progress, downloaded, total ->
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
                isDownloading = false
                downloadJob = null
                val file = targetPath.toFile()
                downloadedFile = file
                with(context) { installPackage(file) }
            } catch (e: Exception) {
                isDownloading = false
                downloadJob = null
                if (e !is CancellationException) {
                    downloadError = e.localizedMessage ?: "Download failed"
                }
            }
        }
    }
}
