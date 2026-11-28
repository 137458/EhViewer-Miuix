package com.ehviewer.core.network

import com.sun.jna.Platform
import com.sun.jna.platform.win32.Crypt32Util
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.io.readByteArray
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer

// 模拟进程重启：save 后以全新 CookiePersistence 实例 load，验证恢复语义
class CookiePersistenceTest {
    private val fs = FileSystem.SYSTEM

    private val secretValue = "ipb_pass_hash_abcdef0123456789"

    private fun newStorePath(tag: String): Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "cookie_persist_${tag}_${System.nanoTime()}.dat"

    private fun record(name: String = "ipb_pass_hash", value: String = secretValue) = CookieJsonRecord(
        name = name,
        value = value,
        domain = "e-hentai.org",
        path = "/",
        secure = true,
        hostOnly = false,
        expiresAtEpochMs = 1_900_000_000_000L,
    )

    private fun unwrap(bytes: ByteArray): ByteArray = if (Platform.isWindows()) Crypt32Util.cryptUnprotectData(bytes) else bytes

    @Test
    fun saveThenLoadRestoresRecordsFromJsonStore() {
        val storePath = newStorePath("roundtrip")
        try {
            CookiePersistence(storePath).save(listOf(record(), record(name = "ipb_member_id", value = "42")))

            val storedBytes = fs.source(storePath).buffer().use { it.readByteArray() }
            // Windows 落盘为 DPAPI 密文：明文 cookie 不得直接出现在字节流中（非 Windows 为可读 JSON）
            if (Platform.isWindows()) {
                assertFalse(storedBytes.asIsoString().contains(secretValue), "cookie value must not be stored in plaintext")
            }
            // 解密后必须是 JSON 文本，而不是 Java 序列化流
            val plainText = unwrap(storedBytes).decodeToString()
            assertTrue(plainText.trimStart().startsWith("["), "store must be JSON, was: ${plainText.take(32)}")

            // 新实例 = 新进程语义：不共享内存态，仅依赖磁盘文件
            val restored = CookiePersistence(storePath).load()
            assertEquals(2, restored.size)
            assertEquals(secretValue, restored.single { it.name == "ipb_pass_hash" }.value)
            assertEquals(1_900_000_000_000L, restored.single { it.name == "ipb_pass_hash" }.expiresAtEpochMs)
            assertEquals("e-hentai.org", restored.single { it.name == "ipb_pass_hash" }.domain)
        } finally {
            fs.delete(storePath)
        }
    }

    @Test
    fun loadLegacyJavaSerializedStoreMigratesAndRewritesAsJson() {
        val storePath = newStorePath("legacy")
        try {
            // 旧版 save() 落盘的是 flatMap 产出的 ArrayList；严格输入过滤器只放行 ArrayList
            val legacy = ArrayList<CookieRecord>().apply {
                add(
                    CookieRecord(
                        uri = URI("https://e-hentai.org"),
                        name = "ipb_member_id",
                        value = "legacy",
                        domain = ".e-hentai.org",
                        path = "/",
                        secure = false,
                    ),
                )
            }
            val legacyBytes = java.io.ByteArrayOutputStream().also { buffer ->
                java.io.ObjectOutputStream(buffer).use { it.writeObject(legacy) }
            }.toByteArray()
            // 旧版在 Windows 上写的是 DPAPI 密文，非 Windows 是明文序列化流
            val stored = if (Platform.isWindows()) Crypt32Util.cryptProtectData(legacyBytes) else legacyBytes
            fs.sink(storePath).buffer().use { it.write(stored) }

            val reader = CookiePersistence(storePath)
            val restored = reader.load()
            assertEquals(1, restored.size)
            assertEquals("legacy", restored.single().value)
            assertEquals("e-hentai.org", restored.single().domain)
            assertFalse(restored.single().hostOnly, ".e-hentai.org 是域 cookie，不是 host-only")

            // 迁移必须立即回写：随后读到的密文内容已是 JSON，旧序列化流不残留
            val rewritten = unwrap(fs.source(storePath).buffer().use { it.readByteArray() }).decodeToString()
            assertTrue(rewritten.trimStart().startsWith("["), "legacy store must be rewritten as JSON")
        } finally {
            fs.delete(storePath)
        }
    }

    @Test
    fun windowsUndecryptableFileIsNotDeserializedAsPlaintext() {
        if (!Platform.isWindows()) return
        val storePath = newStorePath("nofallback")
        try {
            val legacy = listOf(
                CookieRecord(
                    uri = URI("https://e-hentai.org"),
                    name = "ipb_member_id",
                    value = "legacy",
                    domain = ".e-hentai.org",
                    path = "/",
                    secure = false,
                ),
            )
            java.io.ObjectOutputStream(java.io.FileOutputStream(storePath.toString())).use { it.writeObject(legacy) }

            // DPAPI 解密失败（文件不是本账户密文）必须视为损坏并丢弃：绝不回退明文反序列化，
            // 否则任意投放序列化流即可注入可控 cookie
            assertTrue(
                CookiePersistence(storePath).load().isEmpty(),
                "undecryptable store must degrade to empty, never plaintext-deserialize",
            )
        } finally {
            fs.delete(storePath)
        }
    }

    @Test
    fun loadCorruptedFileFallsBackToEmpty() {
        val storePath = newStorePath("corrupt")
        try {
            fs.sink(storePath).buffer().use { it.writeUtf8("garbage-not-json") }
            assertTrue((fs.metadataOrNull(storePath)?.size ?: 0L) > 0L)

            assertTrue(
                CookiePersistence(storePath).load().isEmpty(),
                "corrupted store must degrade to empty, not crash",
            )
        } finally {
            fs.delete(storePath)
        }
    }

    @Test
    fun saveLeavesNoTemporaryFileBehind() {
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "cookie_persist_atomic_${System.nanoTime()}"
        val storePath = dir / "cookies.dat"
        try {
            CookiePersistence(storePath).save(listOf(record()))
            val names = fs.list(dir).map { it.name }
            assertEquals(listOf("cookies.dat"), names, "atomic write must not leave temp files behind: $names")
        } finally {
            fs.deleteRecursively(dir)
        }
    }

    @Test
    fun nonWindowsStoreIsOwnerReadWriteOnly() {
        if (Platform.isWindows()) return
        val storePath = newStorePath("perms")
        try {
            CookiePersistence(storePath).save(listOf(record()))
            val perms = Files.getPosixFilePermissions(Paths.get(storePath.toString()))
            assertEquals(setOf(java.nio.file.attribute.PosixFilePermission.OWNER_READ, java.nio.file.attribute.PosixFilePermission.OWNER_WRITE), perms)
        } finally {
            fs.delete(storePath)
        }
    }
}

private fun ByteArray.asIsoString(): String = String(this, Charsets.ISO_8859_1)
