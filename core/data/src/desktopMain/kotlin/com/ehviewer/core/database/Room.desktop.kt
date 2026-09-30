package com.ehviewer.core.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import okio.Path
import okio.Path.Companion.toPath

actual inline fun <reified T : RoomDatabase> roomDb(
    name: String,
    builder: RoomDatabase.Builder<T>.() -> Unit,
) = Room.databaseBuilder<T>(name)
    .setDriver(BundledSQLiteDriver())
    .apply(builder)
    .build()

// 桌面端数据目录：优先 Windows %APPDATA%，回退 ~/.ehviewer
actual fun getDatabasePath(name: String): Path {
    val base = System.getenv("APPDATA")?.replace('\\', '/')
        ?: (System.getProperty("user.home") + "/.ehviewer")
    return "$base/EhViewer/databases/$name".toPath()
}
