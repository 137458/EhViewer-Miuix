package com.ehviewer.core.preferences

import com.ehviewer.core.desktop.testing.clearIsolatedDataDir
import com.ehviewer.core.desktop.testing.newIsolatedDataDir
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.io.readByteArray
import okio.FileSystem
import okio.Path

// 桌面侧 DataStorePreferences actual 的运行时验证：写读回环 + 真实落盘
class DataStorePreferencesDesktopTest {
    private class TestPrefs(name: String) : DataStorePreferences(name) {
        var greeting by stringPref("greeting", "")
    }

    @Test
    fun writeThenReadRoundtripAndPersistToDisk() {
        val dataDir = newIsolatedDataDir()
        try {
            val name = "smoke_ds_${System.nanoTime()}"
            val prefs = TestPrefs(name)
            prefs.greeting = "EhViewer desktop smoke"
            assertEquals("EhViewer desktop smoke", prefs.greeting)

            // 写入是异步落盘的，轮询等待；落盘位置必须落在重定向后的数据根内（测试隔离）
            val fs = FileSystem.SYSTEM
            val file: Path = preferencesPath(name)
            assertTrue(
                file.toString().startsWith(dataDir.toString()),
                "preferences should stay inside isolated data dir: $file",
            )
            val deadline = System.currentTimeMillis() + 5000
            while ((fs.metadataOrNull(file)?.size ?: 0L) == 0L && System.currentTimeMillis() < deadline) {
                Thread.sleep(50)
            }
            val size = fs.metadataOrNull(file)?.size ?: 0L
            assertTrue(size > 0L, "preferences file should be persisted at $file")

            // 落盘内容应包含键名，证明写入的是结构化偏好而非空壳文件
            val bytes = fs.read(file) { readByteArray() }
            assertTrue(String(bytes, Charsets.ISO_8859_1).contains("greeting"))
        } finally {
            clearIsolatedDataDir()
        }
    }
}
