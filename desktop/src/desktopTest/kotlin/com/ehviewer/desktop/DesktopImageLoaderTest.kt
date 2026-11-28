package com.ehviewer.desktop

import coil3.PlatformContext
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DesktopImageLoaderTest {
    @Test
    fun defaultCacheDirectoryLivesUnderCacheRoot() {
        val cacheRoot = DesktopDirs.cacheRoot
        // 图片缓存落在桌面端缓存根下（Windows: %APPDATA%/EhViewer/cache，Linux: XDG_CACHE_HOME）
        assertEquals(
            File(cacheRoot, "image_cache").absolutePath,
            DesktopImageLoader.defaultCacheDirectory().absolutePath,
        )
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
    fun cacheSizeForCreatesDirectoryBeforeMeasuring() {
        val root = createTempDirectory("coil-size").toFile()
        try {
            val cacheDir = File(root, "nested/image_cache")
            assertFalse(cacheDir.exists())
            val size = DesktopImageLoader.cacheSizeFor(cacheDir)
            // 先 mkdirs 再取 usableSpace：目录不存在时 usableSpace 返回 0 会把配额压到最小档
            assertTrue(cacheDir.isDirectory)
            val mb = 1024L * 1024
            assertTrue(
                size == 128 * mb || size == 256 * mb || size == 512 * mb,
                "配额必须落在既有三档之内，实际: $size",
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun nearestExistingWalksUpToExistingAncestor() {
        val root = createTempDirectory("coil-nearest").toFile()
        try {
            val deep = File(root, "a/b/c")
            assertEquals(root.canonicalPath, DesktopImageLoader.nearestExisting(deep)?.canonicalPath)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun diskCacheBuilderHonoursSizeAndDirectory() {
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
    fun newImageLoaderUsesInjectedCacheDirectory() {
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
