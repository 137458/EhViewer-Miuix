package com.ehviewer.core

import okio.Path
import okio.Path.Companion.toPath

// 桌面端数据根目录：优先 Windows %APPDATA%，回退 ~/.ehviewer。
// ehviewer.data.dir 系统属性可整体重定向根目录（desktopTest 依赖此隔离真实用户数据）。
internal object DesktopDirs {
    private const val OVERRIDE_PROPERTY = "ehviewer.data.dir"

    val root: Path
        get() {
            val base = System.getProperty(OVERRIDE_PROPERTY)?.takeIf { it.isNotBlank() }?.replace('\\', '/')
                ?: System.getenv("APPDATA")?.replace('\\', '/')
                ?: (System.getProperty("user.home") + "/.ehviewer")
            return "$base/EhViewer".toPath()
        }

    val databasesDir: Path get() = root / "databases"

    val filesDir: Path get() = root / "files"
}
