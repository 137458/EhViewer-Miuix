package com.ehviewer.desktop

import coil3.PlatformContext
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DesktopImageLoaderTest {

    @Test
    fun testDefaultCacheDirectory() {
        val base = System.getProperty("ehviewer.data.dir")?.takeIf { it.isNotBlank() }
            ?: System.getenv("APPDATA")
            ?: (System.getProperty("user.home") + File.separator + ".ehviewer")
        val expected = File(base, "EhViewer" + File.separator + "cache" + File.separator + "image_cache")
        val actual = DesktopImageLoader.defaultCacheDirectory()
        assertEquals(expected.canonicalPath, actual.canonicalPath)
    }

    @Test
    fun adaptiveCacheSizeMatchesAndroidTiers() {
        val gb = 1024L * 1024 * 1024
        val mb = 1024L * 1024
        // 与 Android 侧 EhApplication.thumbCache 三档公式一致：>10G→512M，>2G→256M，否则 128M
        assertEquals(512 * mb, DesktopImageLoader.adaptiveCacheSizeBytes(11 * gb))
        // 边界：恰好 10G 不满足 >10G → 256M；恰好 2G 不满足 >2G → 128M
        assertEquals(256 * mb, DesktopImageLoader.adaptiveCacheSizeBytes(10 * gb))
        assertEquals(256 * mb, DesktopImageLoader.adaptiveCacheSizeBytes(3 * gb))
        assertEquals(128 * mb, DesktopImageLoader.adaptiveCacheSizeBytes(2 * gb))
        assertEquals(128 * mb, DesktopImageLoader.adaptiveCacheSizeBytes(500 * mb))
        // 非法输入回退 256M（与 Android getOrDefault 一致）
        assertEquals(256 * mb, DesktopImageLoader.adaptiveCacheSizeBytes(-1L))
    }

    @Test
    fun testDiskCacheBuilder() {
        val tempDir = createTempDirectory("coil-test-cache").toFile()
        try {
            val diskCache = DesktopImageLoader.buildDiskCache(tempDir, maxSizeBytes = 10 * 1024 * 1024)
            assertEquals(10 * 1024 * 1024L, diskCache.maxSize)
            assertEquals(tempDir.canonicalPath, diskCache.directory.toFile().canonicalPath)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testNewImageLoaderConfig() {
        val tempDir = createTempDirectory("coil-loader-test").toFile()
        try {
            val loader = DesktopImageLoader.newImageLoader(
                context = PlatformContext.INSTANCE,
                cacheDir = tempDir,
            )
            val diskCache = loader.diskCache
            assertNotNull(diskCache)
            assertEquals(tempDir.canonicalPath, diskCache.directory.toFile().canonicalPath)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
