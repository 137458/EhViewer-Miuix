package com.ehviewer.core.files

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.io.readString
import kotlinx.io.writeString
import okio.FileSystem

// 桌面侧 FileSystem/Path.read/write actual 的运行时验证
class FileSystemDesktopTest {
    private val file = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "ehviewer_desktop_test_roundtrip.txt"

    @Test
    fun writeThenReadRoundtrip() {
        val content = "EhViewer desktop filesystem roundtrip 你好"
        try {
            file.write {
                writeString(content)
            }
            assertTrue(file.exists())
            assertTrue(file.isFile)
            assertEquals(content, file.read { readString() })
        } finally {
            file.delete()
        }
        assertFalse(file.exists())
    }
}
