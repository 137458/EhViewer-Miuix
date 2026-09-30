package com.ehviewer.core.database

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
}
