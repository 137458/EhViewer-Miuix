package com.ehviewer.desktop

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import okio.FileSystem
import okio.Path.Companion.toPath

class DesktopImageSaverTest {
    private fun tempDir(): okio.Path = Files.createTempDirectory("eh_saver_test").toFile().absolutePath.toPath()

    @Test
    fun fileNameDerivedFromUrlLastSegment() {
        assertEquals(
            "123-456.webp",
            DesktopImageSaver.fileNameForUrl("https://ei.example/t/abc/123-456.webp", gid = 42L, page = 3),
        )
        assertEquals(
            "cover.jpg",
            DesktopImageSaver.fileNameForUrl("https://ehgt.org/w/01/cover.jpg", gid = 42L, page = 1),
        )
    }

    @Test
    fun fileNameFallsBackWithoutUsableExtension() {
        // 无扩展名/空段回退 eh_{gid}_p{page}.jpg
        assertEquals(
            "eh_42_p3.jpg",
            DesktopImageSaver.fileNameForUrl("https://e-hentai.org/fullimage?q=x", gid = 42L, page = 3),
        )
        assertEquals(
            "eh_42_p1.jpg",
            DesktopImageSaver.fileNameForUrl("https://e-hentai.org/", gid = 42L, page = 1),
        )
    }

    @Test
    fun illegalNameCharactersSanitized() {
        assertEquals(
            "a_b_c.jpg",
            DesktopImageSaver.fileNameForUrl("https://x/a:b\\c.jpg", gid = 1L, page = 1),
        )
    }

    @Test
    fun uniqueTargetAppendsCounterOnCollision() {
        val fs = FileSystem.SYSTEM
        val dir = tempDir()

        // 无冲突：原名
        assertEquals(dir / "pic.jpg", DesktopImageSaver.uniqueTarget(dir, "pic.jpg", fs))
        fs.write(dir / "pic.jpg") { }
        assertEquals(dir / "pic(1).jpg", DesktopImageSaver.uniqueTarget(dir, "pic.jpg", fs))
        fs.write(dir / "pic(1).jpg") { }
        assertEquals(dir / "pic(2).jpg", DesktopImageSaver.uniqueTarget(dir, "pic.jpg", fs))
    }

    @Test
    fun uniqueTargetMissingDirectoryFails() {
        val fs = FileSystem.SYSTEM
        val absent = Files.createTempDirectory("eh_saver_absent").toFile()
            .resolve("absent").absolutePath.toPath()
        assertFailsWith<java.io.IOException> {
            DesktopImageSaver.uniqueTarget(absent, "a.jpg", fs)
        }
    }
}
