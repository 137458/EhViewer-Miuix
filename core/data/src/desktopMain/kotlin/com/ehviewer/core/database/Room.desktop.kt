package com.ehviewer.core.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ehviewer.core.DesktopDirs
import okio.Path

// 桌面端数据库目录与偏好/Cookie 同源（DesktopDirs）
@PublishedApi
internal val databasesDir: Path get() = DesktopDirs.databasesDir

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
