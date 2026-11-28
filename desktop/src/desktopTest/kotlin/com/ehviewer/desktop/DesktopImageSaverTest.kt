package com.ehviewer.desktop

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import okio.FileSystem
import okio.Path.Companion.toPath

class DesktopImageSaverTest {
    private fun tempDir(tag: String): okio.Path = Files.createTempDirectory("eh_saver_$tag").toFile().absolutePath.toPath()

    private fun <T> withTempDir(tag: String, block: (okio.Path) -> T): T {
        val dir = tempDir(tag)
        try {
            return block(dir)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

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
    fun windowsReservedNamesGetPrefix() {
        // CON/NUL 等保留名在 Windows 上无法创建，落盘前统一加前缀
        assertEquals("_CON.jpg", DesktopImageSaver.fileNameForUrl("https://x/CON.jpg", gid = 1L, page = 1))
        assertEquals("_nul.jpg", DesktopImageSaver.fileNameForUrl("https://x/nul.jpg", gid = 1L, page = 1))
        assertEquals("_LPT1.png", DesktopImageSaver.fileNameForUrl("https://x/LPT1.png", gid = 1L, page = 1))
        // 含保留名但主名更长的不是保留名
        assertEquals("CONSOLE.jpg", DesktopImageSaver.fileNameForUrl("https://x/CONSOLE.jpg", gid = 1L, page = 1))
    }

    @Test
    fun overlyLongNamesAreTruncated() {
        val longName = "a".repeat(300) + ".jpg"
        val sanitized = DesktopImageSaver.sanitizeFileName(longName)
        assertTrue(sanitized.length <= 160, "文件名应被截断，实际: $sanitized")
        assertTrue(sanitized.endsWith(".jpg"))
    }

    @Test
    fun uniqueTargetAppendsCounterOnCollision() {
        withTempDir("collision") { dir ->
            val fs = FileSystem.SYSTEM
            // 无冲突：原名
            assertEquals(dir / "pic.jpg", DesktopImageSaver.uniqueTarget(dir, "pic.jpg", fs))
            fs.write(dir / "pic.jpg") { }
            assertEquals(dir / "pic(1).jpg", DesktopImageSaver.uniqueTarget(dir, "pic.jpg", fs))
            fs.write(dir / "pic(1).jpg") { }
            assertEquals(dir / "pic(2).jpg", DesktopImageSaver.uniqueTarget(dir, "pic.jpg", fs))
        }
    }

    @Test
    fun uniqueTargetMissingDirectoryFails() {
        val fs = FileSystem.SYSTEM
        val dir = tempDir("absent_root")
        try {
            val absent = dir / "absent"
            assertFailsWith<java.io.IOException> {
                DesktopImageSaver.uniqueTarget(absent, "a.jpg", fs)
            }
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun uniqueTargetRejectsPathTraversal() {
        withTempDir("traversal") { dir ->
            val fs = FileSystem.SYSTEM
            // 名称必须是单段文件名：任何分隔符或 ".." 都不能把目标移出保存目录
            assertFailsWith<java.io.IOException> { DesktopImageSaver.uniqueTarget(dir, "../escaped.jpg", fs) }
            assertFailsWith<java.io.IOException> { DesktopImageSaver.uniqueTarget(dir, "sub/escaped.jpg", fs) }
            assertFailsWith<java.io.IOException> { DesktopImageSaver.uniqueTarget(dir, "sub\\escaped.jpg", fs) }
            assertFailsWith<java.io.IOException> { DesktopImageSaver.uniqueTarget(dir, "..", fs) }
            assertFailsWith<java.io.IOException> { DesktopImageSaver.uniqueTarget(dir, "   ", fs) }
        }
    }

    @Test
    fun uniqueTargetStaysInsideDirectory() {
        withTempDir("inside") { dir ->
            val fs = FileSystem.SYSTEM
            val target = DesktopImageSaver.uniqueTarget(dir, "a.jpg", fs)
            assertEquals(dir, target.parent)
        }
    }

    @Test
    fun resolveSaveDirPrefersConfiguredDirectory() {
        val home = Files.createTempDirectory("eh_saver_home").toString()
        val configured = Files.createTempDirectory("eh_saver_configured")
        try {
            val target = configured.resolve("pics").toString()
            // 实现统一正斜杠风格，期望值同构
            assertEquals(
                target.replace('\\', '/').toPath(),
                DesktopImageSaver.resolveSaveDir(configured = target, userHome = home),
            )
        } finally {
            configured.toFile().deleteRecursively()
        }
    }

    @Test
    fun resolveSaveDirFallsBackToDownloadsWhenUnconfiguredOrInvalid() {
        val home = Files.createTempDirectory("eh_saver_home2").toString()
        // 未配置：默认下载目录
        assertEquals("$home/Downloads/EhViewer".toPath(), DesktopImageSaver.resolveSaveDir(configured = null, userHome = home))
        // 配置不可创建（含非法字符）：回落下载目录
        assertEquals(
            "$home/Downloads/EhViewer".toPath(),
            DesktopImageSaver.resolveSaveDir(configured = "\u0000illegal", userHome = home),
        )
        // 空白配置等同未配置
        assertEquals("$home/Downloads/EhViewer".toPath(), DesktopImageSaver.resolveSaveDir(configured = "   ", userHome = home))
    }
}
