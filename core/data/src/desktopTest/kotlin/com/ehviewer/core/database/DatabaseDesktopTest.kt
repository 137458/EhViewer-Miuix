package com.ehviewer.core.database

import com.ehviewer.core.database.model.GalleryEntity
import com.ehviewer.core.database.model.LocalFavoriteInfo
import com.ehviewer.core.database.model.QuickSearch
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okio.FileSystem

// 桌面侧 Room actual 的运行时验证：临时目录建全量数据库 + DAO 往返
class DatabaseDesktopTest {
    @Test
    fun roomDbCreatesMissingParentDirectories() {
        // 父目录不存在时建库，验证 actual 兜底创建目录（BundledSQLiteDriver 自身不会建目录）
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "ehviewer_db_dirs_${System.currentTimeMillis()}" / "nested"
        val path = (dir / "missing_dirs.db").toString()
        val db = roomDb<EhDatabase>(path)
        try {
            runBlocking {
                withTimeout(10_000) {
                    db.quickSearchDao().insert(QuickSearch(name = "dirs"))
                    assertEquals(1, db.quickSearchDao().list().size)
                }
            }
        } finally {
            runCatching { db.close() }
            runCatching { File(path).delete() }
        }
    }

    @Test
    fun insertAndListQuickSearchSmoke() {
        val path = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "ehviewer_smoke_${System.currentTimeMillis()}.db").toString()
        val db = roomDb<EhDatabase>(path)
        try {
            runBlocking {
                withTimeout(10_000) {
                    val dao = db.quickSearchDao()
                    dao.insert(QuickSearch(name = "smoke"))
                    val list = dao.list()
                    assertEquals(1, list.size)
                    assertEquals("smoke", list[0].name)
                }
            }
        } finally {
            runCatching { db.close() }
            runCatching { File(path).delete() }
        }
    }

    @Test
    fun insertAndListLocalFavoritesSmoke() {
        val path = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "ehviewer_fav_smoke_${System.currentTimeMillis()}.db").toString()
        val db = roomDb<EhDatabase>(path)
        try {
            runBlocking {
                withTimeout(10_000) {
                    val gallery = GalleryEntity(
                        gid = 12345L,
                        token = "abcde",
                        title = "Favorite Title",
                        titleJpn = "Fav Title Jpn",
                        thumbKey = "thumb.jpg",
                        category = 2,
                        posted = "2026-10-01",
                        uploader = "tester",
                        rating = 5.0f,
                        simpleTags = listOf("favorite"),
                        pages = 50,
                        simpleLanguage = "English",
                        favoriteSlot = -1,
                    )
                    db.galleryDao().upsert(gallery)
                    db.localFavoritesDao().upsert(LocalFavoriteInfo(12345L))

                    val galleries = db.localFavoritesDao().listGalleries()
                    assertEquals(1, galleries.size)
                    assertEquals(12345L, galleries[0].gid)
                    assertEquals("Favorite Title", galleries[0].title)

                    // 删除测试
                    db.localFavoritesDao().deleteByKey(12345L)
                    assertEquals(0, db.localFavoritesDao().listGalleries().size)
                }
            }
        } finally {
            runCatching { db.close() }
            runCatching { File(path).delete() }
        }
    }
}
