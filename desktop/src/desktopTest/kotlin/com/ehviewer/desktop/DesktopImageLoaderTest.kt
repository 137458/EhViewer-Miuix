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
