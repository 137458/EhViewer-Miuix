package com.ehviewer.desktop

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopDirsTest {
    @Test
    fun windowsUsesRoamingAppData() {
        assertEquals(
            File("C:/Users/u/AppData/Roaming", DesktopDirs.APP_DIR_NAME).path,
            DesktopDirs.resolveDataRoot(
                overrideDir = null,
                appData = "C:/Users/u/AppData/Roaming",
                xdgDataHome = "/home/u/.local/share",
                userHome = "C:/Users/u",
                osName = "Windows 11",
            ).path,
        )
        assertEquals(
            File("C:/Users/u/AppData/Roaming", "${DesktopDirs.APP_DIR_NAME}/cache").path,
            DesktopDirs.resolveCacheRoot(
                overrideDir = null,
                appData = "C:/Users/u/AppData/Roaming",
                xdgCacheHome = "/home/u/.cache",
                userHome = "C:/Users/u",
                osName = "Windows 11",
            ).path,
        )
    }

    @Test
    fun linuxHonorsXdgVariables() {
        assertEquals(
            File("/xdg/data", "ehviewer").path,
            DesktopDirs.resolveDataRoot(null, null, "/xdg/data", "/home/u", "Linux").path,
        )
        assertEquals(
            File("/xdg/cache", "ehviewer").path,
            DesktopDirs.resolveCacheRoot(null, null, "/xdg/cache", "/home/u", "Linux").path,
        )
        // 无 XDG_DATA_HOME 时退到 ~/.ehviewer/EhViewer；无 XDG_CACHE_HOME 时退到 ~/.cache
        assertEquals(
            File("/home/u", ".ehviewer/${DesktopDirs.APP_DIR_NAME}").path,
            DesktopDirs.resolveDataRoot(null, null, null, "/home/u", "Linux").path,
        )
        assertEquals(
            File("/home/u", ".cache/ehviewer").path,
            DesktopDirs.resolveCacheRoot(null, null, null, "/home/u", "Linux").path,
        )
    }

    @Test
    fun macOsUsesLibraryConventions() {
        assertEquals(
            File("/Users/u", "Library/Application Support/${DesktopDirs.APP_DIR_NAME}").path,
            DesktopDirs.resolveDataRoot(null, null, null, "/Users/u", "Mac OS X").path,
        )
        assertEquals(
            File("/Users/u", "Library/Caches/${DesktopDirs.APP_DIR_NAME}").path,
            DesktopDirs.resolveCacheRoot(null, null, null, "/Users/u", "Mac OS X").path,
        )
    }

    @Test
    fun explicitOverrideWinsOnEveryPlatform() {
        // ehviewer.data.dir 覆盖优先于平台默认（既有安装路径不迁移）
        assertEquals(
            File("/data", DesktopDirs.APP_DIR_NAME).path,
            DesktopDirs.resolveDataRoot("/data", "C:/AppData", "/xdg", "/home/u", "Windows 11").path,
        )
        assertEquals(
            File("/data", "${DesktopDirs.APP_DIR_NAME}/cache").path,
            DesktopDirs.resolveCacheRoot("/data", "C:/AppData", "/xdg", "/home/u", "Windows 11").path,
        )
        // 空白覆盖等同未设置
        assertEquals(
            File("/xdg/data", "ehviewer").path,
            DesktopDirs.resolveDataRoot("   ", null, "/xdg/data", "/home/u", "Linux").path,
        )
    }

    @Test
    fun cacheFallsBackUnderDataRootWithoutHome() {
        assertEquals(
            File(File("C:/AppData", DesktopDirs.APP_DIR_NAME), "cache").path,
            DesktopDirs.resolveCacheRoot(null, "C:/AppData", null, null, "Windows 11").path,
        )
    }
}
