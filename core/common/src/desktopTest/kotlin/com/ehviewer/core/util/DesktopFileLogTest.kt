package com.ehviewer.core.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import okio.FileSystem
import okio.Path.Companion.toPath

class DesktopFileLogTest {
    private fun tempDir() = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "eh_filelog_${System.nanoTime()}"

    @Test
    fun appendCreatesFileAndAppendsLines() {
        val fs = FileSystem.SYSTEM
        val file = tempDir() / "ehviewer.log"
        DesktopFileLog.append(file, "line-1", fs)
        DesktopFileLog.append(file, "line-2", fs)
        val content = fs.read(file) { readUtf8() }
        assertEquals("line-1\nline-2\n", content)
    }

    @Test
    fun rotateKeepsOneGeneration() {
        val fs = FileSystem.SYSTEM
        val file = tempDir() / "ehviewer.log"
        DesktopFileLog.append(file, "old", fs)
        // 模拟超限：直接断言轮转边界行为（用小文件无法到 1MB，改为验证轮转后原文件重建）
        fs.write(file) { writeUtf8("x".repeat(DesktopFileLog.MAX_BYTES.toInt())) }
        DesktopFileLog.append(file, "new", fs)
        // 原内容滚到 .log.1，新文件仅含新行
        assertTrue(fs.exists(file.parent!! / "ehviewer.log.1"))
        assertEquals("new\n", fs.read(file) { readUtf8() })
    }
}
