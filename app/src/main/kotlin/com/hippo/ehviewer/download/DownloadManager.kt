/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.download

import android.net.Uri
import android.util.SparseLongArray
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.util.size
import arrow.fx.coroutines.parMapNotNull
import com.ehviewer.core.data.model.asEntity
import com.ehviewer.core.database.model.DownloadArtist
import com.ehviewer.core.database.model.DownloadInfo
import com.ehviewer.core.database.model.DownloadLabel
import com.ehviewer.core.files.delete
import com.ehviewer.core.files.find
import com.ehviewer.core.files.toOkioPath
import com.ehviewer.core.files.toUri
import com.ehviewer.core.model.BaseGalleryInfo
import com.ehviewer.core.model.GalleryInfo
import com.ehviewer.core.preferences.edit
import com.ehviewer.core.util.LogPriority
import com.ehviewer.core.util.logcat
import com.ehviewer.core.util.mapNotNull
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.exception.FatalException
import com.hippo.ehviewer.spider.COMIC_INFO_FILE
import com.hippo.ehviewer.spider.SpeedTracker
import com.hippo.ehviewer.spider.SpiderQueen
import com.hippo.ehviewer.spider.SpiderQueen.Companion.SPIDER_INFO_FILENAME
import com.hippo.ehviewer.spider.SpiderQueen.OnSpiderListener
import com.hippo.ehviewer.spider.downloadDirname
import com.hippo.ehviewer.spider.readComicInfo
import com.hippo.ehviewer.spider.readCompatFromPath
import com.hippo.ehviewer.spider.toSimpleTags
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.insertWith
import com.hippo.ehviewer.util.runAssertingNotMainThread
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import moe.tarsin.coroutines.runSuspendCatching
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath

object DownloadManager : OnSpiderListener, CoroutineScope {
    override val coroutineContext = Dispatchers.IO + Job()

    // All download info list
    private val allInfoList = runAssertingNotMainThread { EhDB.getAllDownloadInfo() as MutableList }

    // 对外只暴露不可变快照，避免调用方绕过锁直接改内部表
    val downloadInfoList: List<DownloadInfo>
        get() = allInfoList.toList()

    // All download info map
    // 被下载线程与 UI 线程同时读写，必须用并发实现
    private val mAllInfoMap = ConcurrentHashMap<Long, DownloadInfo>(allInfoList.size).apply {
        allInfoList.forEach { put(it.gid, it) }
    }

    // All labels without default label
    // Create the SnapshotStateList first in case the database query is time-consuming
    val labelList = mutableStateListOf<DownloadLabel>().apply {
        addAll(runAssertingNotMainThread { EhDB.getAllDownloadLabelList() })
    }

    // Store download info wait to start
    private val mWaitList = ArrayDeque<DownloadInfo>()

    // onPageDownload 是每 chunk 都会走的热路径：只投递样本，由单个消费者协程提交给 mSpeedReminder
    private val speedUpdates = Channel<SpeedUpdate>(Channel.UNLIMITED)

    private class SpeedUpdate(
        val index: Int,
        val contentLength: Long,
        val receivedSize: Long,
        val bytesRead: Int,
    )

    private var mDownloadListener: DownloadListener? = null
    private var mCurrentTask: DownloadInfo? = null
    private var mCurrentSpider: SpiderQueen? = null

    private val mutableNotifyFlow = MutableSharedFlow<DownloadInfo>(extraBufferCapacity = 1)
    val notifyFlow = mutableNotifyFlow.asSharedFlow()

    private val mutex = Mutex()

    private val sortMutex = Mutex()

    // labelList 的位次维护（新增/改名/删除）统一用这把锁串起来，避免插入位次撞车
    private val labelMutex = Mutex()
    private val sortMode
        get() = SortMode.from(Settings.downloadSortMode.value)

    var isInitialized by mutableStateOf(false)
        private set

    init {
        launch {
            val mode = sortMode
            if (mode != SortMode.Default) sortDownloads(mode)
            isInitialized = true
        }
        launch {
            for (update in speedUpdates) {
                mSpeedReminder.onDownload(update.index, update.contentLength, update.receivedSize, update.bytesRead)
            }
        }
    }

    fun containLabel(label: String?): Boolean {
        if (label == null) {
            return false
        }
        return labelList.any { it.label == label }
    }

    fun containDownloadInfo(gid: Long) = mAllInfoMap.containsKey(gid)

    fun getDownloadInfo(gid: Long) = mAllInfoMap[gid]

    fun getDownloadState(gid: Long): Int {
        val info = mAllInfoMap[gid]
        return info?.state ?: DownloadInfo.STATE_INVALID
    }

    fun setDownloadListener(listener: DownloadListener?) {
        mDownloadListener = listener
    }

    // 服务销毁时只清掉自己注册的监听器，避免把后来者的监听器一起抹掉
    fun clearDownloadListener(listener: DownloadListener) {
        if (mDownloadListener === listener) {
            mDownloadListener = null
        }
    }

    private suspend fun ensureDownload() {
        mutex.withLock {
            if (mCurrentTask != null) {
                // Only one download
                return
            }

            // Get download from wait list
            if (!mWaitList.isEmpty()) {
                val info = mWaitList.removeFirst()
                mCurrentTask = info
                with(info) {
                    archiveFile?.let {
                        return onFinish(pages, pages, pages, failed = false)
                    }
                    state = DownloadInfo.STATE_DOWNLOAD
                    speed = -1
                    remaining = -1
                    total = -1
                    finished = 0
                    downloaded = 0
                    legacy = -1
                }
                val spider = SpiderQueen.obtainSpiderQueen(info, SpiderQueen.MODE_DOWNLOAD)
                mCurrentSpider = spider
                spider.addOnSpiderListener(this)
                // Update in DB
                EhDB.putDownloadInfo(info)
                // Start speed count
                mSpeedReminder.start()
                // Notify start downloading
                mDownloadListener?.onStart(info)
                // Notify state update
                mutableNotifyFlow.emit(info)
            }
        }
    }

    suspend fun startDownload(galleryInfo: BaseGalleryInfo, label: String?) {
        val infoToNotify = mutex.withLock {
            if (mCurrentTask?.gid == galleryInfo.gid) {
                return@withLock null
            }

            // Check in download list
            var info = mAllInfoMap[galleryInfo.gid]
            if (info != null) { // Get it in download list
                if (info.state != DownloadInfo.STATE_WAIT) {
                    // Set state DownloadInfo.STATE_WAIT
                    info.state = DownloadInfo.STATE_WAIT
                    // Add to wait list
                    mWaitList.add(info)
                    // Update in DB
                    EhDB.putDownloadInfo(info)
                    info
                } else {
                    null
                }
            } else {
                // It is new download info
                info = DownloadInfo(galleryInfo.asEntity(), galleryInfo.downloadDirname())
                info.label = label
                info.state = DownloadInfo.STATE_WAIT
                // Add to all download list and map
                sortMutex.withLock {
                    allInfoList.insertWith(info, sortMode.comparator())
                }
                mAllInfoMap[galleryInfo.gid] = info

                // Add to wait list
                mWaitList.add(info)

                // Save to
                EhDB.putDownloadInfo(info)

                // Add it to history
                EhDB.putHistoryInfo(info)
                info
            }
        }
        if (infoToNotify != null) {
            mutableNotifyFlow.emit(infoToNotify)
            ensureDownload()
        }
    }

    suspend fun startRangeDownload(gidList: LongArray) {
        // 状态切换与入队必须在 mutex 内一起完成，否则会和 ensureDownload 抢走同一个 info
        val updateList = mutex.withLock {
            gidList.mapNotNull { mAllInfoMap[it] }
                .filter {
                    when (it.state) {
                        DownloadInfo.STATE_NONE, DownloadInfo.STATE_FAILED, DownloadInfo.STATE_FINISH -> true
                        else -> false
                    }
                }
                .onEach {
                    it.state = DownloadInfo.STATE_WAIT
                    mWaitList.add(it)
                }
        }
        if (updateList.isNotEmpty()) {
            updateList.onEach {
                EhDB.putDownloadInfo(it)
                mutableNotifyFlow.emit(it)
            }
            ensureDownload()
        }
    }

    @Stable
    @Composable
    fun collectDownloadState(gid: Long): State<Int> = remember {
        notifyFlow.transform { if (it.gid == gid) emit(getDownloadState(gid)) }
    }.collectAsState(getDownloadState(gid))

    @Stable
    @Composable
    fun collectContainDownloadInfo(gid: Long): State<Boolean> = remember {
        notifyFlow.transform { if (it.gid == gid) emit(containDownloadInfo(gid)) }
    }.collectAsState(containDownloadInfo(gid))

    @Stable
    @Composable
    inline fun <T> updatedDownloadInfo(info: DownloadInfo, crossinline transform: @DisallowComposableCalls DownloadInfo.() -> T): T = remember {
        notifyFlow.transform { if (it.gid == info.gid) emit(transform(it)) }
    }.collectAsState(transform(info)).value

    suspend fun startAllDownload() {
        val updateList = mutex.withLock {
            sortMutex.withLock {
                allInfoList.filter {
                    when (it.state) {
                        DownloadInfo.STATE_NONE, DownloadInfo.STATE_FAILED -> true
                        else -> false
                    }
                }
            }.onEach {
                it.state = DownloadInfo.STATE_WAIT
                mWaitList.add(it)
            }
        }
        if (updateList.isNotEmpty()) {
            updateList.onEach {
                EhDB.putDownloadInfo(it)
                mutableNotifyFlow.emit(it)
            }
            ensureDownload()
        }
    }

    suspend fun addDownload(downloadInfoList: List<DownloadInfo>) = sortMutex.withLock {
        val comparator = sortMode.comparator()
        downloadInfoList.forEach { info ->
            if (containDownloadInfo(info.gid)) return@forEach

            // Ensure download state
            if (DownloadInfo.STATE_WAIT == info.state || DownloadInfo.STATE_DOWNLOAD == info.state) {
                info.state = DownloadInfo.STATE_NONE
            }

            // Add to all download list and map
            allInfoList.insertWith(info, comparator)
            mAllInfoMap[info.gid] = info

            // Save to
            EhDB.putDownloadInfo(info)
            EhDB.putDownloadArtist(info.gid, info.artistInfoList)
        }
    }

    suspend fun addDownloadLabel(downloadLabelList: List<DownloadLabel>) = labelMutex.withLock {
        // 位次要接在已有标签之后（而不是本批次的长度），否则第二次导入会和第一次撞位次
        downloadLabelList.forEach { label ->
            if (!containLabel(label.label)) {
                label.position = labelList.size
                labelList.add(EhDB.addDownloadLabel(label))
            }
        }
    }

    suspend fun restoreDownload(galleryInfo: BaseGalleryInfo, dirname: String) {
        val info = DownloadInfo(galleryInfo.asEntity(), dirname)
        info.state = DownloadInfo.STATE_NONE

        // Add to all download list and map
        sortMutex.withLock {
            allInfoList.insertWith(info, sortMode.comparator())
        }
        mAllInfoMap[galleryInfo.gid] = info

        // Save to
        EhDB.putDownloadInfo(info)

        // Notify
        mutableNotifyFlow.emit(info)
    }

    suspend fun stopDownload(gid: Long) {
        val info = stopDownloadInternal(gid)
        if (info != null) {
            // Update listener
            mutableNotifyFlow.emit(info)
            // Ensure download
            ensureDownload()
        }
    }

    suspend fun stopRangeDownload(gidList: LongArray) {
        stopRangeDownloadInternal(gidList)

        // Update listener
        gidList.mapNotNull { mAllInfoMap[it] }.forEach {
            mutableNotifyFlow.emit(it)
        }

        // Ensure download
        ensureDownload()
    }

    suspend fun stopAllDownload() {
        mutex.withLock {
            // Stop all in wait list
            for (info in mWaitList) {
                info.state = DownloadInfo.STATE_NONE
                // Update in DB
                EhDB.putDownloadInfo(info)

                // Notify
                mutableNotifyFlow.emit(info)
            }
            mWaitList.clear()

            // Stop current
            val info = stopCurrentDownloadInternalLocked()
            info?.let { mutableNotifyFlow.emit(it) }
        }
    }

    suspend fun deleteDownload(gid: Long, deleteFiles: Boolean = false) {
        stopDownloadInternal(gid)
        val info = mAllInfoMap[gid]
        if (info != null) {
            // Remove from DB
            EhDB.removeDownloadInfo(info)

            // Remove all list and map
            sortMutex.withLock {
                allInfoList.remove(info)
            }
            mAllInfoMap.remove(info.gid)

            // Update listener
            mutableNotifyFlow.emit(info)

            // Ensure download
            ensureDownload()

            if (deleteFiles) {
                info.downloadDir?.delete()
                info.tempDownloadDir?.delete()
                EhDB.removeDownloadDirname(info.gid)
            }
        }
    }

    suspend fun deleteRangeDownload(gidList: LongArray) {
        stopRangeDownloadInternal(gidList)
        val list = gidList.mapNotNull { gid ->
            mAllInfoMap.remove(gid)
        }
        EhDB.removeDownloadInfo(list)
        sortMutex.withLock {
            allInfoList.removeAll(list.toSet())
        }

        // Update listener
        list.forEach { mutableNotifyFlow.emit(it) }

        // Ensure download
        ensureDownload()
    }

    suspend fun sortDownloads(mode: SortMode) = sortMutex.withLock {
        allInfoList.sortWith(mode.comparator())
    }

    suspend fun resetAllReadingProgress() = runSuspendCatching {
        EhDB.clearProgressInfo()
    }.onFailure { logcat(it) }

    // Update in DB
    // Update listener
    // No ensureDownload
    private suspend fun stopDownloadInternal(gid: Long): DownloadInfo? = mutex.withLock {
        stopDownloadInternalLocked(gid)
    }

    private suspend fun stopDownloadInternalLocked(gid: Long): DownloadInfo? {
        // Check current task
        if (mCurrentTask?.gid == gid) {
            // Stop current
            return stopCurrentDownloadInternalLocked()
        }
        val iterator = mWaitList.iterator()
        while (iterator.hasNext()) {
            val info = iterator.next()
            if (info.gid == gid) {
                // Remove from wait list
                iterator.remove()
                // Update state
                info.state = DownloadInfo.STATE_NONE
                // Update in DB
                EhDB.putDownloadInfo(info)
                return info
            }
        }
        return null
    }

    // Update in DB
    // Update mDownloadListener
    private suspend fun stopCurrentDownloadInternalLocked(): DownloadInfo? {
        val info = mCurrentTask
        val spider = mCurrentSpider
        // Release spider
        if (spider != null) {
            mCurrentSpider = null
            spider.removeOnSpiderListener(this@DownloadManager)
            SpiderQueen.releaseSpiderQueen(spider, SpiderQueen.MODE_DOWNLOAD)
        }
        mCurrentTask = null
        // Stop speed reminder
        mSpeedReminder.stop()
        if (info == null) {
            return null
        }

        // Update state
        info.state = DownloadInfo.STATE_NONE
        // Update in DB
        EhDB.putDownloadInfo(info)
        // Listener
        mDownloadListener?.onCancel(info)
        return info
    }

    // Update in DB
    // Update mDownloadListener
    private suspend fun stopRangeDownloadInternal(gidList: LongArray) = mutex.withLock {
        // Two way
        if (gidList.size < mWaitList.size) {
            for (element in gidList) {
                stopDownloadInternalLocked(element)
            }
        } else {
            // Check current task
            if (mCurrentTask != null && gidList.contains(mCurrentTask!!.gid)) {
                // Stop current
                stopCurrentDownloadInternalLocked()
            }

            // Check all in wait list
            val iterator = mWaitList.iterator()
            while (iterator.hasNext()) {
                val info = iterator.next()
                if (gidList.contains(info.gid)) {
                    // Remove from wait list
                    iterator.remove()
                    // Update state
                    info.state = DownloadInfo.STATE_NONE
                    // Update in DB
                    EhDB.putDownloadInfo(info)
                }
            }
        }
    }

    /**
     * @param label Not allow new label
     */
    suspend fun changeLabel(list: Collection<DownloadInfo>, label: String?) {
        if (null != label && !containLabel(label)) {
            logcat(TAG, LogPriority.ERROR) { "Not exits label: $label" }
            return
        }
        list.forEach {
            it.label = label
        }
        EhDB.updateDownloadInfo(list)
    }

    suspend fun addLabel(label: String?) = labelMutex.withLock {
        if (label == null || containLabel(label)) {
            return@withLock
        }
        labelList.add(EhDB.addDownloadLabel(DownloadLabel(label, labelList.size)))
    }

    suspend fun renameLabel(from: String, to: String) = labelMutex.withLock {
        val index = labelList.indexOfFirst { it.label == from }
        if (index != -1) {
            val exist = labelList.removeAt(index)
            val new = exist.copy(label = to)
            labelList.add(index, new)
            EhDB.updateDownloadLabel(new)
            sortMutex.withLock {
                allInfoList.forEach {
                    if (it.label == from) {
                        it.label = to
                    }
                }
            }
        }
    }

    suspend fun deleteLabel(label: String) {
        val (removed, shifted) = labelMutex.withLock {
            removeLabelAndShiftPositions(labelList, label)
        }
        if (removed != null) {
            EhDB.removeDownloadLabel(removed)
            shifted.forEach { EhDB.updateDownloadLabel(it) }
        }
        sortMutex.withLock {
            allInfoList.forEach {
                if (it.label == label) {
                    it.label = null
                }
            }
        }
    }

    suspend fun readMetadataFromLocal() {
        val list = sortMutex.withLock {
            allInfoList.mapNotNull {
                val updateGallery = it.pages == 0 || it.simpleTags == null
                val updateArtist = it.artistInfoList.isEmpty()
                if (updateGallery || updateArtist) {
                    Triple(it, updateGallery, updateArtist)
                } else {
                    null
                }
            }
        }.parMapNotNull(concurrency = 5) { (info, updateGallery, updateArtist) ->
            info.downloadDir?.run {
                val comicInfo = find(COMIC_INFO_FILE)?.let { readComicInfo(it) }
                if (comicInfo != null) {
                    val galleryInfo = if (updateGallery) {
                        info.pages = comicInfo.pageCount
                        info.simpleTags = comicInfo.toSimpleTags()
                        info.galleryInfo
                    } else {
                        null
                    }

                    val artistList = if (updateArtist && comicInfo.penciller != null) {
                        info.artistInfoList = DownloadArtist.from(info.gid, comicInfo.penciller)
                        info.gid to info.artistInfoList
                    } else {
                        null
                    }

                    if (galleryInfo != null || artistList != null) {
                        galleryInfo to artistList
                    } else {
                        null
                    }
                } else if (info.pages == 0) {
                    val galleryInfo = find(SPIDER_INFO_FILENAME)?.let {
                        readCompatFromPath(it)?.run {
                            info.pages = pages
                            info.galleryInfo
                        }
                    }

                    galleryInfo?.let { it to null }
                } else {
                    null
                }
            }
        }

        val galleryInfoList = buildList {
            list.forEach { (info, artists) ->
                info?.let {
                    add(it)
                }
                artists?.let { (gid, updateList) ->
                    EhDB.putDownloadArtist(gid, updateList)
                }
            }
        }
        if (galleryInfoList.isNotEmpty()) EhDB.updateGalleryInfo(galleryInfoList)
    }

    val isIdle: Boolean
        get() = mCurrentTask == null && mWaitList.isEmpty()

    override fun onGetPages(pages: Int) {
        launch {
            mCurrentTask?.let { info ->
                info.total = pages
                mutableNotifyFlow.emit(info)
            } ?: logcat(TAG, LogPriority.ERROR) { "Current task is null, but it should not be" }
        }
    }

    override fun onFatal(error: FatalException) {
        launch {
            mDownloadListener?.onFatal(error)
            stopAllDownload()
        }
    }

    override fun onPageDownload(
        index: Int,
        contentLength: Long,
        receivedSize: Long,
        bytesRead: Int,
    ) {
        speedUpdates.trySend(SpeedUpdate(index, contentLength, receivedSize, bytesRead))
    }

    override fun onPageSuccess(index: Int, finished: Int, downloaded: Int, total: Int) {
        launch {
            mSpeedReminder.onDone(index)
            mCurrentTask?.let { info ->
                info.finished = finished
                info.downloaded = downloaded
                info.total = total
                mDownloadListener?.onDownload(info)
                mutableNotifyFlow.emit(info)
            } ?: logcat(TAG, LogPriority.ERROR) { "Current task is null, but it should not be" }
        }
    }

    override fun onPageFailure(
        index: Int,
        error: String?,
        finished: Int,
        downloaded: Int,
        total: Int,
    ) {
        launch {
            mSpeedReminder.onDone(index)
            mCurrentTask?.let { info ->
                info.finished = finished
                info.downloaded = downloaded
                info.total = total
                mutableNotifyFlow.emit(info)
            } ?: logcat(TAG, LogPriority.ERROR) { "Current task is null, but it should not be" }
        }
    }

    override fun onFinish(finished: Int, downloaded: Int, total: Int, failed: Boolean) {
        launch {
            var finishedInfo: DownloadInfo? = null
            mutex.withLock {
                mSpeedReminder.onFinish()
                mCurrentSpider?.let { spider ->
                    mCurrentSpider = null
                    spider.removeOnSpiderListener(DownloadManager)
                    SpiderQueen.releaseSpiderQueen(spider, SpiderQueen.MODE_DOWNLOAD)
                }
                mCurrentTask?.let { info ->
                    mCurrentTask = null
                    mSpeedReminder.stop()
                    info.finished = finished
                    info.downloaded = downloaded
                    info.total = total
                    info.legacy = total - finished
                    info.state = if (failed) DownloadInfo.STATE_FAILED else DownloadInfo.STATE_FINISH
                    EhDB.putDownloadInfo(info)
                    finishedInfo = info
                } ?: logcat(TAG, LogPriority.ERROR) { "Current task is null, but it should not be" }
            }
            finishedInfo?.let { info ->
                mDownloadListener?.onFinish(info)
                mutableNotifyFlow.emit(info)
                // 只有真的结束了一个任务才推进等待队列
                ensureDownload()
            }
        }
    }

    interface DownloadListener {
        /**
         * Fatal error
         */
        fun onFatal(error: FatalException)

        /**
         * Start download
         */
        fun onStart(info: DownloadInfo)

        /**
         * Update download speed
         */
        fun onDownload(info: DownloadInfo)

        /**
         * Download done
         */
        fun onFinish(info: DownloadInfo)

        /**
         * Download done
         */
        fun onCancel(info: DownloadInfo)
    }

    private val mSpeedReminder = object {
        private val contentLengthMap = SparseLongArray()
        private val receivedSizeMap = SparseLongArray()
        private val tracker = SpeedTracker()
        private val mutex = Mutex()
        private var currentJob: Job? = null
        fun start() {
            if (currentJob == null) {
                currentJob = launch {
                    tracker.speedFlow().collect(::updateSpeed)
                }
            }
        }

        suspend fun stop() {
            tracker.reset()
            mutex.withLock {
                contentLengthMap.clear()
                receivedSizeMap.clear()
            }
            currentJob?.cancel()
            currentJob = null
        }

        suspend fun onDownload(index: Int, contentLength: Long, receivedSize: Long, bytesRead: Int) {
            mutex.withLock {
                contentLengthMap.put(index, contentLength)
                receivedSizeMap.put(index, receivedSize)
            }
            tracker.track(bytesRead)
        }

        suspend fun onDone(index: Int) {
            mutex.withLock {
                contentLengthMap.delete(index)
                receivedSizeMap.delete(index)
            }
        }

        suspend fun onFinish() {
            mutex.withLock {
                contentLengthMap.clear()
                receivedSizeMap.clear()
            }
        }

        private suspend fun updateSpeed(speed: Long) {
            mCurrentTask?.let { info ->
                info.speed = speed

                // Calculate remaining
                if (info.total <= 0) {
                    info.remaining = -1
                } else if (speed == 0L) {
                    info.remaining = 300L * 24L * 60L * 60L * 1000L // 300 days
                } else {
                    var downloadingCount = 0
                    var downloadingContentLengthSum = 0L
                    var totalSize = 0L
                    mutex.withLock {
                        for (i in 0..<contentLengthMap.size) {
                            val contentLength = contentLengthMap.valueAt(i)
                            val receivedSize = receivedSizeMap.valueAt(i)
                            // 长度未知（-1）的分块不能参与剩余时间估算，否则分母被污染
                            if (contentLength > 0) {
                                downloadingCount++
                                downloadingContentLengthSum += contentLength
                                totalSize += contentLength - receivedSize
                            }
                        }
                    }
                    if (downloadingCount != 0) {
                        totalSize += downloadingContentLengthSum * (info.total - info.downloaded - downloadingCount) / downloadingCount
                        info.remaining = totalSize / speed * 1000
                    }
                }
                mDownloadListener?.onDownload(info)
                mutableNotifyFlow.emit(info)
            }
        }
    }

    private const val TAG = "DownloadManager"
}

var downloadLocation: Path
    get() = with(Settings) {
        if (downloadScheme != null) {
            Uri.Builder().apply {
                scheme(downloadScheme)
                encodedAuthority(downloadAuthority)
                encodedPath(downloadPath)
                encodedQuery(downloadQuery)
                encodedFragment(downloadFragment)
            }.build().toOkioPath()
        } else {
            AppConfig.defaultDownloadDir?.toOkioPath() ?: "".toPath()
        }
    }
    set(value) = with(value.toUri()) {
        Settings.edit {
            downloadScheme = scheme
            downloadAuthority = encodedAuthority
            downloadPath = encodedPath
            downloadQuery = encodedQuery
            downloadFragment = encodedFragment
        }
    }

// dirname 可能来自旧版本数据库：含路径分隔符或 .. 时会跑出下载目录，这类值按"没有目录"处理
internal fun isSafeDownloadDirname(dirname: String?): Boolean = !dirname.isNullOrEmpty() && dirname != "." && dirname != ".." &&
    !dirname.contains('/') && !dirname.contains('\\')

val DownloadInfo.downloadDir get() = dirname?.takeIf(::isSafeDownloadDirname)?.let { downloadLocation / it }
val DownloadInfo.archiveFile get() = downloadDir?.run { find("$gid.cbz") ?: find("$gid.zip") }
val GalleryInfo.tempDownloadDir get() = AppConfig.externalTempPersistDir?.let { it / "$gid" }

internal fun removeLabelAndShiftPositions(
    list: MutableList<DownloadLabel>,
    targetLabel: String,
): Pair<DownloadLabel?, List<DownloadLabel>> {
    val index = list.indexOfFirst { it.label == targetLabel }
    if (index == -1) return null to emptyList()
    val item = list.removeAt(index)
    val shifted = ArrayList<DownloadLabel>(list.size - index)
    for (i in index until list.size) {
        val current = list[i]
        current.position--
        shifted.add(current)
    }
    return item to shifted
}
