package com.ehviewer.core.database

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.FileSystem

/**
 * H-09：GALLERIES_FTS 是 content=`GALLERIES` 的 FTS4 内容表，索引不由内容表自动回填。
 * 历史用户升级后可能出现「内容表有数据、FTS 索引为空」的状态，此时标题检索永远无命中，
 * 因此迁移必须执行 rebuild。这里用手工构造的旧版本库验证 23 -> 24 迁移会重建索引。
 */
class DatabaseFtsMigrationTest {
    @Test
    fun migrationRebuildsEmptyGalleryFtsIndex() {
        val path = (
            FileSystem.SYSTEM_TEMPORARY_DIRECTORY /
                "ehviewer_fts_migration_${System.currentTimeMillis()}.db"
            ).toString()
        try {
            createLegacyDatabase(path, version = 23)
            // 迁移前索引为空：内容表能查到行，但 MATCH 无命中（复现升级后索引未重建的状态）
            assertEquals(1, galleryRowCount(path))
            assertEquals(0, ftsMatchCount(path, "needle"))

            val db = roomDb<EhDatabase>(path)
            try {
                // 打开数据库即触发迁移
                runBlocking { withTimeout(20_000) { db.quickSearchDao().list() } }
            } finally {
                runCatching { db.close() }
            }

            assertEquals(1, ftsMatchCount(path, "needle"))
        } finally {
            runCatching { File(path).delete() }
        }
    }
}

// 旧版本 schema 由 Room 导出的 schema json 还原，避免手写 DDL 与 exporter 漂移
private fun createLegacyDatabase(path: String, version: Int) {
    val database = exportedSchema(version)
    val entities = database.getValue("entities").jsonArray.map { it.jsonObject }
    val driver = BundledSQLiteDriver()
    val connection = driver.open(path)
    try {
        val contents = entities.single { it.stringValue("tableName") == "GALLERIES" }
        connection.execSQL(contents.ddl("GALLERIES"))
        // 内容表先写入数据，再建 FTS 表与同步触发器：索引因此为空
        connection.execSQL(
            "INSERT INTO GALLERIES (GID, TOKEN, TITLE, CATEGORY, RATING, FAVORITE_SLOT) " +
                "VALUES (1, 'token1', 'Alpha Needle', 0, 4.5, 0)",
        )
        entities.forEach { entity ->
            val tableName = entity.stringValue("tableName")
            connection.execSQL(entity.ddl(tableName))
            entity["indices"]?.jsonArray?.forEach { connection.execSQL(it.jsonObject.ddl(tableName)) }
            entity["contentSyncTriggers"]?.jsonArray?.forEach {
                connection.execSQL(it.jsonPrimitive.content)
            }
        }
        connection.execSQL("PRAGMA user_version = $version")
    } finally {
        connection.close()
    }
}

private fun exportedSchema(version: Int): JsonObject {
    val relative = "schemas/com.ehviewer.core.database.EhDatabase/$version.json"
    val file = listOf(File(relative), File("core/data/$relative"), File("../$relative"))
        .firstOrNull { it.isFile }
        ?: error("Room schema json not found: $relative (cwd=${File(".").absolutePath})")
    return Json.parseToJsonElement(file.readText()).jsonObject.getValue("database").jsonObject
}

private fun JsonObject.stringValue(key: String): String = getValue(key).jsonPrimitive.content

private fun JsonObject.ddl(tableName: String): String = stringValue("createSql").replace("\${TABLE_NAME}", tableName)

private fun galleryRowCount(path: String): Long = withConnection(path) { connection ->
    connection.count("SELECT count(*) FROM GALLERIES")
}

private fun ftsMatchCount(path: String, term: String): Long = withConnection(path) { connection ->
    connection.count("SELECT count(*) FROM GALLERIES_FTS WHERE GALLERIES_FTS MATCH '$term'")
}

private fun androidx.sqlite.SQLiteConnection.count(sql: String): Long {
    val statement = prepare(sql)
    return try {
        check(statement.step()) { "no row returned: $sql" }
        statement.getLong(0)
    } finally {
        statement.close()
    }
}

private fun <T> withConnection(path: String, block: (androidx.sqlite.SQLiteConnection) -> T): T {
    val connection = BundledSQLiteDriver().open(path)
    return try {
        block(connection)
    } finally {
        connection.close()
    }
}
