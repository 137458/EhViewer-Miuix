package com.ehviewer.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.ehviewer.core.database.model.GalleryEntity
import com.ehviewer.core.database.model.HistoryInfo
import com.ehviewer.core.database.model.HistoryTime
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT HISTORY.* FROM HISTORY JOIN GALLERIES USING(GID) ORDER BY TIME")
    suspend fun list(): List<HistoryInfo>

    // 桌面等无 Paging 环境的一次性历史画廊快照（带标题等画廊字段）
    @Query("SELECT GALLERIES.* FROM HISTORY JOIN GALLERIES USING(GID) ORDER BY TIME DESC")
    suspend fun listGalleries(): List<GalleryEntity>

    // 桌面响应式历史画廊流（增删改自动推送，替代手动刷新）
    @Query("SELECT GALLERIES.* FROM HISTORY JOIN GALLERIES USING(GID) ORDER BY TIME DESC")
    fun listGalleriesFlow(): Flow<List<GalleryEntity>>

    // 桌面历史浏览时间流（GID→最后浏览 epoch millis），供列表展示
    @Query("SELECT HISTORY.GID AS gid, HISTORY.TIME AS time FROM HISTORY ORDER BY TIME DESC")
    fun listTimesFlow(): Flow<List<HistoryTime>>

    @Query("SELECT GALLERIES.* FROM HISTORY JOIN GALLERIES USING(GID) ORDER BY TIME DESC")
    fun joinListLazy(): PagingSource<Int, GalleryEntity>

    @Query(
        """SELECT GALLERIES.* FROM HISTORY JOIN GALLERIES USING(GID)
        JOIN GALLERIES_FTS ON GALLERIES.rowid = docid WHERE GALLERIES_FTS MATCH :title ORDER BY TIME DESC""",
    )
    fun joinListLazy(title: String): PagingSource<Int, GalleryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(historyInfoList: List<HistoryInfo>)

    @Upsert
    suspend fun upsert(historyInfo: HistoryInfo)

    @Query("DELETE FROM HISTORY WHERE GID = :gid")
    suspend fun deleteByKey(gid: Long)

    @Query("DELETE FROM HISTORY")
    suspend fun deleteAll()

    @Query("SELECT EXISTS(SELECT * FROM HISTORY WHERE GID = :gid)")
    suspend fun contains(gid: Long): Boolean
}
