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
package com.hippo.ehviewer

import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room.execSQL
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.ehviewer.core.data.model.asEntity
import com.ehviewer.core.database.EhDatabase
import com.ehviewer.core.database.Schema17to18
import com.ehviewer.core.database.getDatabasePath
import com.ehviewer.core.database.model.DownloadArtist
import com.ehviewer.core.database.model.DownloadDirname
import com.ehviewer.core.database.model.DownloadInfo
import com.ehviewer.core.database.model.DownloadLabel
import com.ehviewer.core.database.model.Filter
import com.ehviewer.core.database.model.GalleryEntity
import com.ehviewer.core.database.model.HistoryInfo
import com.ehviewer.core.database.model.LocalFavoriteInfo
import com.ehviewer.core.database.model.ProgressInfo
import com.ehviewer.core.database.model.QuickSearch
import com.ehviewer.core.database.roomDb
import com.ehviewer.core.files.delete
import com.ehviewer.core.files.read
import com.ehviewer.core.files.sendTo
import com.ehviewer.core.model.GalleryInfo
import com.ehviewer.core.util.logcat
import com.hippo.ehviewer.download.DownloadManager
import kotlinx.coroutines.flow.Flow
import kotlinx.io.readByteArray
import moe.tarsin.coroutines.runSuspendCatching
import okio.Path

object EhDB {
    private const val DB_NAME = "eh.db"
    private val db = roomDb<EhDatabase>(DB_NAME) {
        addMigrations(Schema17to18())
    }

    suspend fun putGalleryInfo(galleryInfo: GalleryEntity) {
        db.galleryDao().upsert(galleryInfo)
    }

    suspend fun cleanOrphanGallery(gid: Long) {
        val inDownloads = db.downloadsDao().contains(gid)
        val inLocalFav = db.localFavoritesDao().contains(gid)
        val inHistory = db.historyDao().contains(gid)
        if (shouldDeleteGallery(inDownloads, inLocalFav, inHistory)) {
            db.galleryDao().deleteByKey(gid)
        }
    }

    private suspend fun deleteGalleryInfo(galleryInfo: GalleryEntity) {
        cleanOrphanGallery(galleryInfo.gid)
    }

    suspend fun updateGalleryInfo(galleryInfoList: List<GalleryEntity>) {
        db.galleryDao().update(galleryInfoList)
    }

    fun getReadProgressFlow(gid: Long) = db.progressDao().getPageFlow(gid)
    suspend fun getReadProgress(gid: Long) = db.progressDao().getPage(gid)
    suspend fun putReadProgress(gid: Long, page: Int) {
        // PROGRESS has a foreign key to GALLERIES, so progress for a gallery that is no longer
        // stored would abort with a constraint violation. Skip it instead of failing the write.
        if (db.galleryDao().load(gid) == null) {
            logcat("EhDB") { "Skip read progress for unknown gallery $gid" }
            return
        }
        db.progressDao().upsert(ProgressInfo(gid, page))
    }
    suspend fun clearProgressInfo() = db.progressDao().deleteAll()

    suspend fun getAllDownloadInfo() = db.downloadsDao().joinList().onEach {
        if (it.state == DownloadInfo.STATE_WAIT || it.state == DownloadInfo.STATE_DOWNLOAD) {
            it.state = DownloadInfo.STATE_NONE
        }
    }

    suspend fun updateDownloadInfo(downloadInfo: Collection<DownloadInfo>) {
        val dao = db.downloadsDao()
        dao.update(downloadInfo.map(DownloadInfo::downloadInfo))
    }

    suspend fun putDownloadInfo(downloadInfo: DownloadInfo) {
        db.useWriterConnection { conn ->
            conn.immediateTransaction {
                putGalleryInfo(downloadInfo.galleryInfo)
                db.downloadsDao().upsert(downloadInfo.downloadInfo)
            }
        }
    }

    suspend fun removeDownloadInfo(downloadInfo: DownloadInfo) {
        db.useWriterConnection { conn ->
            conn.immediateTransaction {
                val dao = db.downloadsDao()
                dao.delete(downloadInfo.downloadInfo)
                deleteGalleryInfo(downloadInfo.galleryInfo)
            }
        }
    }

    suspend fun randomLocalFav() = db.localFavoritesDao().random()

    suspend fun removeDownloadInfo(downloadInfo: List<DownloadInfo>) {
        db.useWriterConnection { conn ->
            conn.immediateTransaction {
                val dao = db.downloadsDao()
                downloadInfo.forEach {
                    dao.delete(it.downloadInfo)
                    deleteGalleryInfo(it.galleryInfo)
                }
            }
        }
    }

    suspend fun getDownloadDirname(gid: Long): String? {
        val dao = db.downloadDirnameDao()
        val raw = dao.load(gid)
        return raw?.dirname
    }

    suspend fun putDownloadDirname(gid: Long, dirname: String) {
        val dao = db.downloadDirnameDao()
        dao.upsert(DownloadDirname(gid, dirname))
    }

    suspend fun removeDownloadDirname(gid: Long) {
        val dao = db.downloadDirnameDao()
        dao.deleteByKey(gid)
    }

    private suspend fun importDownloadDirname(downloadDirnameList: List<DownloadDirname>) {
        val dao = db.downloadDirnameDao()
        dao.insertOrIgnore(downloadDirnameList)
    }

    val downloadsCountByLabel
        get() = db.downloadsDao().countByLabel()

    val downloadsCountByArtist
        get() = db.downloadsDao().countByArtist()

    suspend fun getAllDownloadLabelList() = db.downloadLabelDao().list()

    suspend fun addDownloadLabel(raw: DownloadLabel): DownloadLabel {
        // Reset id
        raw.id = null
        val dao = db.downloadLabelDao()
        raw.id = dao.insert(raw)
        return raw
    }

    suspend fun updateDownloadLabel(raw: DownloadLabel) {
        val dao = db.downloadLabelDao()
        dao.update(raw)
    }

    suspend fun updateDownloadLabel(downloadLabels: List<DownloadLabel>) {
        val dao = db.downloadLabelDao()
        dao.update(downloadLabels)
    }

    suspend fun removeDownloadLabel(raw: DownloadLabel) {
        db.useWriterConnection { conn ->
            conn.immediateTransaction {
                val dao = db.downloadLabelDao()
                dao.delete(raw)
                dao.fill(raw.position)
            }
        }
    }

    suspend fun searchDownloadLabel(keyword: String, limit: Int) = db.downloadLabelDao().search("%$keyword%", limit)

    suspend fun putDownloadArtist(gid: Long, artists: List<DownloadArtist>) {
        db.useWriterConnection { conn ->
            conn.immediateTransaction {
                val dao = db.downloadArtistDao()
                // Drop the old rows even when the new list is empty, otherwise stale artists survive.
                dao.deleteByGid(gid)
                if (artists.isNotEmpty()) dao.insertOrIgnore(artists)
            }
        }
    }

    suspend fun removeLocalFavorites(galleryInfo: GalleryInfo) {
        db.localFavoritesDao().deleteByKey(galleryInfo.gid)
        deleteGalleryInfo(galleryInfo.asEntity())
    }

    suspend fun removeLocalFavorites(galleryInfoList: Collection<GalleryInfo>) {
        galleryInfoList.forEach {
            removeLocalFavorites(it)
        }
    }

    suspend fun containLocalFavorites(gid: Long): Boolean {
        val dao = db.localFavoritesDao()
        return dao.contains(gid)
    }

    suspend fun putLocalFavorites(galleryInfo: GalleryInfo) {
        db.useWriterConnection { conn ->
            conn.immediateTransaction {
                putGalleryInfo(galleryInfo.asEntity())
                db.localFavoritesDao().upsert(LocalFavoriteInfo(galleryInfo.gid))
            }
        }
    }

    suspend fun putLocalFavorites(galleryInfoList: Collection<GalleryInfo>) {
        galleryInfoList.forEach {
            putLocalFavorites(it)
        }
    }

    private suspend fun importLocalFavorites(localFavorites: List<LocalFavoriteInfo>) {
        db.localFavoritesDao().insertOrIgnore(localFavorites)
    }

    suspend fun getAllQuickSearch() = db.quickSearchDao().list()

    suspend fun insertQuickSearch(quickSearch: QuickSearch) {
        val dao = db.quickSearchDao()
        quickSearch.id = dao.insert(quickSearch)
    }

    private suspend fun importQuickSearch(quickSearchList: List<QuickSearch>) {
        val dao = db.quickSearchDao()
        dao.insert(quickSearchList)
    }

    suspend fun deleteQuickSearch(quickSearch: QuickSearch) {
        db.useWriterConnection { conn ->
            conn.immediateTransaction {
                val dao = db.quickSearchDao()
                dao.delete(quickSearch)
                dao.fill(quickSearch.position)
            }
        }
    }

    suspend fun updateQuickSearch(quickSearchList: List<QuickSearch>) {
        val dao = db.quickSearchDao()
        dao.update(quickSearchList)
    }

    val historyLazyList
        get() = db.historyDao().joinListLazy()

    fun searchHistory(keyword: String): PagingSource<Int, GalleryEntity> = buildFtsQuery(keyword)?.let { db.historyDao().joinListLazy(it) } ?: emptySearchResult()

    val localFavLazyList
        get() = db.localFavoritesDao().joinListLazy()

    val localFavCount: Flow<Int>
        get() = db.localFavoritesDao().count()

    fun searchLocalFav(keyword: String): PagingSource<Int, GalleryEntity> = buildFtsQuery(keyword)?.let { db.localFavoritesDao().joinListLazy(it) } ?: emptySearchResult()

    suspend fun putHistoryInfo(galleryInfo: GalleryInfo) {
        db.useWriterConnection { conn ->
            conn.immediateTransaction {
                putGalleryInfo(galleryInfo.asEntity())
                db.historyDao().upsert(HistoryInfo(galleryInfo.gid))
            }
        }
    }

    suspend fun updateFavoriteSlot(gid: Long, slot: Int) {
        val dao = db.galleryDao()
        dao.load(gid)?.let {
            it.favoriteSlot = slot
            dao.update(it)
        }
    }

    private suspend fun importHistoryInfo(historyInfoList: List<HistoryInfo>) {
        val dao = db.historyDao()
        dao.insertOrIgnore(historyInfoList)
    }

    suspend fun deleteHistoryInfo(galleryInfo: GalleryEntity) {
        db.useWriterConnection { conn ->
            conn.immediateTransaction {
                val dao = db.historyDao()
                dao.deleteByKey(galleryInfo.gid)
                deleteGalleryInfo(galleryInfo)
            }
        }
    }

    suspend fun clearHistoryInfo() {
        db.useWriterConnection { conn ->
            conn.immediateTransaction {
                val dao = db.historyDao()
                val historyList = dao.list()
                dao.deleteAll()
                historyList.forEach { cleanOrphanGallery(it.gid) }
            }
        }
    }

    suspend fun getAllFilter() = db.filterDao().list()

    suspend fun addFilter(filter: Filter): Boolean {
        val existFilter = runSuspendCatching { db.filterDao().load(filter.text, filter.mode.field) }.getOrNull()
        return if (existFilter == null) {
            filter.id = null
            filter.id = db.filterDao().insert(filter)
            true
        } else {
            false
        }
    }

    suspend fun deleteFilter(filter: Filter) {
        db.filterDao().delete(filter)
    }

    suspend fun updateFilter(filter: Filter) {
        db.filterDao().update(filter)
    }

    suspend fun vacuumDB() {
        db.useWriterConnection { conn ->
            conn.execSQL("PRAGMA wal_checkpoint(FULL)")
            conn.execSQL("VACUUM")
        }
    }

    suspend fun exportDB(file: Path) {
        db.useWriterConnection { conn ->
            // TRUNCATE instead of FULL: without it the newest pages could still sit in the -wal file,
            // which is not part of the exported copy.
            conn.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
            conn.execSQL("VACUUM")
            // Copy while the writer connection is held so no write can land in between.
            getDatabasePath(DB_NAME) sendTo file
        }
    }

    suspend fun importDB(file: Path) {
        // Unique name: two imports must never share a stale temp database.
        val tempDBName = "import-${System.currentTimeMillis()}.db"
        val dbFile = getDatabasePath(tempDBName)
        try {
            file sendTo dbFile
            verifySqliteFile(dbFile)
            val oldDB = roomDb<EhDatabase>(tempDBName) {
                addMigrations(Schema17to18())
            }
            try {
                db.useWriterConnection { conn ->
                    conn.immediateTransaction {
                        db.galleryDao().insertOrIgnore(oldDB.galleryDao().list())
                        db.progressDao().insertOrIgnore(oldDB.progressDao().list())
                        importDownloadDirname(oldDB.downloadDirnameDao().list())
                        importHistoryInfo(oldDB.historyDao().list())
                        importQuickSearch(importableQuickSearch(oldDB.quickSearchDao().list()))
                        importLocalFavorites(oldDB.localFavoritesDao().list())
                    }
                }

                // DownloadManager caches download rows outside the database, so these merges run in
                // their own transactions instead of one big write transaction.
                DownloadManager.addDownloadLabel(oldDB.downloadLabelDao().list())
                DownloadManager.addDownload(oldDB.downloadsDao().joinList().asReversed())
                oldDB.filterDao().list().forEach {
                    addFilter(it)
                }
            } finally {
                oldDB.close()
            }
        } finally {
            // Room keeps -wal/-shm siblings; leaving them behind would poison the next import.
            dbFile.delete()
            getDatabasePath("$tempDBName-wal").delete()
            getDatabasePath("$tempDBName-shm").delete()
        }
    }

    // A blank or punctuation only keyword cannot be turned into a MATCH expression, so an empty page
    // keeps the UI consistent instead of failing the query.
    private fun emptySearchResult(): PagingSource<Int, GalleryEntity> = object : PagingSource<Int, GalleryEntity>() {
        override fun getRefreshKey(state: PagingState<Int, GalleryEntity>): Int? = null
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GalleryEntity> = LoadResult.Page(emptyList(), null, null)
    }

    private fun verifySqliteFile(file: Path) {
        val magic = "SQLite format 3\u0000".toByteArray()
        val header = runCatching { file.read { readByteArray(magic.size) } }.getOrNull()
        require(header != null && header.contentEquals(magic)) { "Not a SQLite database: ${file.name}" }
    }

    private suspend fun importableQuickSearch(quickSearchList: List<QuickSearch>): List<QuickSearch> {
        val currentQuickSearchList = db.quickSearchDao().list()
        val offset = currentQuickSearchList.size
        return quickSearchList.filter { newQS ->
            currentQuickSearchList.none { it.name == newQS.name }
        }.onEachIndexed { index, q ->
            q.id = null
            q.position = index + offset
        }
    }
}
