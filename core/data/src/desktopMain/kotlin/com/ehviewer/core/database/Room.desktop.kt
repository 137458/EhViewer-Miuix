package com.ehviewer.core.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import okio.Path
import okio.Path.Companion.toPath

// 桌面端数据目录：优先 Windows %APPDATA%，回退 ~/.ehviewer
@PublishedApi
internal val databasesDir: Path by lazy {
    val base = System.getenv("APPDATA")?.replace('\\', '/')
        ?: (System.getProperty("user.home") + "/.ehviewer")
    "$base/EhViewer/databases".toPath()
}

actual fun getDatabasePath(name: String): Path = databasesDir / name

actual inline fun <reified T : RoomDatabase> roomDb(
    name: String,
    builder: RoomDatabase.Builder<T>.() -> Unit,
): T {
    // 调用方语义与 Android 一致：传纯文件名时落到数据目录；已是完整路径则原样使用
    val path = if (name.contains('/') || name.contains('\\')) {
        name
    } else {
        (databasesDir / name).toString()
    }
    return Room.databaseBuilder<T>(path)
        .setDriver(BundledSQLiteDriver())
        .apply(builder)
        .build()
}
