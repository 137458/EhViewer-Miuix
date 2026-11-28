package com.ehviewer.desktop

import java.io.File

// 桌面端数据/缓存根目录。
// core 模块里的 DesktopDirs 是 internal（跨模块不可见），且只覆盖数据根，
// 故桌面侧自带一份：数据根规则与 core 保持一致（已有用户目录不迁移），
// 缓存根额外按平台走 XDG / macOS 约定，避免 Linux 上把图片缓存塞进 ~/.ehviewer。
object DesktopDirs {
    const val APP_DIR_NAME: String = "EhViewer"
    const val LOCK_FILE_NAME: String = ".lock"

    private val osName: String get() = System.getProperty("os.name", "")

    // 数据根：ehviewer.data.dir 覆盖 > %APPDATA% > macOS Application Support > XDG_DATA_HOME > ~/.ehviewer
    val dataRoot: File by lazy {
        resolveDataRoot(
            overrideDir = System.getProperty("ehviewer.data.dir"),
            appData = System.getenv("APPDATA"),
            xdgDataHome = System.getenv("XDG_DATA_HOME"),
            userHome = System.getProperty("user.home"),
            osName = osName,
        )
    }

    // 缓存根：可重建、可清理，不随数据备份迁移
    val cacheRoot: File by lazy {
        resolveCacheRoot(
            overrideDir = System.getProperty("ehviewer.data.dir"),
            appData = System.getenv("APPDATA"),
            xdgCacheHome = System.getenv("XDG_CACHE_HOME"),
            userHome = System.getProperty("user.home"),
            osName = osName,
        )
    }

    // 单实例锁文件：跟随数据根（用户私有目录，可写）
    fun lockFile(): File = File(dataRoot, LOCK_FILE_NAME)

    internal fun resolveDataRoot(
        overrideDir: String?,
        appData: String?,
        xdgDataHome: String?,
        userHome: String?,
        osName: String?,
    ): File {
        overrideDir?.takeIf { it.isNotBlank() }?.let { return File(it, APP_DIR_NAME) }
        val home = userHome?.takeIf { it.isNotBlank() }
        if (isWindows(osName)) {
            appData?.takeIf { it.isNotBlank() }?.let { return File(it, APP_DIR_NAME) }
        }
        if (isMac(osName)) {
            home?.let { return File(it, "Library/Application Support/$APP_DIR_NAME") }
        } else {
            xdgDataHome?.takeIf { it.isNotBlank() }?.let { return File(it, "ehviewer") }
        }
        home?.let { return File(it, ".ehviewer/$APP_DIR_NAME") }
        // 兜底：连 user.home 都缺失时退到相对路径（可写性由调用方 mkdirs 结果暴露）
        return File(".", "$APP_DIR_NAME")
    }

    internal fun resolveCacheRoot(
        overrideDir: String?,
        appData: String?,
        xdgCacheHome: String?,
        userHome: String?,
        osName: String?,
    ): File {
        overrideDir?.takeIf { it.isNotBlank() }?.let { return File(it, "$APP_DIR_NAME/cache") }
        val home = userHome?.takeIf { it.isNotBlank() }
        if (isWindows(osName)) {
            appData?.takeIf { it.isNotBlank() }?.let { return File(it, "$APP_DIR_NAME/cache") }
        }
        if (isMac(osName)) {
            home?.let { return File(it, "Library/Caches/$APP_DIR_NAME") }
        } else {
            xdgCacheHome?.takeIf { it.isNotBlank() }?.let { return File(it, "ehviewer") }
            home?.let { return File(it, ".cache/ehviewer") }
        }
        // 无 HOME（极端环境）：缓存退到数据根下，至少与数据同生命周期
        return File(resolveDataRoot(overrideDir, appData, null, userHome, osName), "cache")
    }

    private fun isWindows(osName: String?): Boolean = osName.orEmpty().startsWith("Windows", ignoreCase = true)

    private fun isMac(osName: String?): Boolean = osName.orEmpty().startsWith("Mac", ignoreCase = true)
}
