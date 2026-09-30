package com.ehviewer.core.network

import java.net.HttpCookie
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.io.readByteArray
import okio.FileSystem
import okio.buffer

// 模拟进程重启：save 后以全新 CookiePersistence 实例 load，验证恢复语义
class CookiePersistenceTest {
    private val fs = FileSystem.SYSTEM

    @Test
    fun saveThenLoadRestoresEntriesLikeNewProcess() {
        val storePath = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "cookie_persist_${System.nanoTime()}.dat"
        val writer = CookiePersistence(storePath)
        val uri = URI("https://e-hentai.org")
        val cookie = HttpCookie("ipb_member_id", "42").apply {
            domain = ".e-hentai.org"
            this.path = "/"
        }
        writer.save(mapOf(uri to listOf(cookie)))

        // 新实例 = 新进程语义：不共享内存态，仅依赖磁盘文件
        val reader = CookiePersistence(storePath)
        val restored = reader.load()
        val restoredCookie = restored[uri]?.single { it.getName() == "ipb_member_id" }
        assertEquals("42", restoredCookie?.getValue())
        assertEquals(".e-hentai.org", restoredCookie?.getDomain())
    }

    @Test
    fun loadCorruptedFileFallsBackToEmpty() {
        val storePath = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "cookie_persist_corrupt_${System.nanoTime()}.dat"
        fs.sink(storePath).buffer().use { it.writeUtf8("garbage-not-java-serialized") }
        assertTrue((fs.metadataOrNull(storePath)?.size ?: 0L) > 0)

        val reader = CookiePersistence(storePath)
        runBlocking {
            withTimeout(5_000) {
                assertTrue(reader.load().isEmpty(), "corrupted store must degrade to empty, not crash")
            }
        }
        fs.delete(storePath)
    }
}
