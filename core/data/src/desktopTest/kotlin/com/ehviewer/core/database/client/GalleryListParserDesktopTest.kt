package com.ehviewer.core.database.client

import com.ehviewer.core.desktop.testing.clearIsolatedDataDir
import com.ehviewer.core.desktop.testing.newIsolatedDataDir
import com.hippo.ehviewer.client.parser.parseGalleryList
import com.hippo.ehviewer.client.parser.rustGalleryBindingsAvailable
import io.ktor.utils.io.core.toByteArray
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertTrue

// Rust 原生列表解析绑定的冒烟验证：
// dll 可用时，垃圾输入必须在 Rust 解析层失败（而非 JNI 符号缺失），
// 以此证明符号绑定链路成立；无 dll 的环境（CI/Linux）跳过。
class GalleryListParserDesktopTest {
    @Test
    fun nativeBindingResolvesAndRejectsGarbageInput() {
        val dataDir = newIsolatedDataDir()
        try {
            // 构造 Direct ByteBuffer 模拟真实网络响应
            val bytes = "<html><body>not a gallery list</body></html>".toByteArray()
            val directBuffer = ByteBuffer.allocateDirect(bytes.size).put(bytes).apply { flip() }

            val thrown = runCatching { parseGalleryList(directBuffer) }.exceptionOrNull()
            assertTrue(thrown != null, "garbage input must fail at parse or bindings layer")
            assertTrue(
                thrown !is UnsatisfiedLinkError,
                "failure must NOT be an UnsatisfiedLinkError: $thrown",
            )
            // 如果原生绑定可用，异常消息应为解析失败或语义校验失败，而非类未初始化
            assertTrue(
                thrown is IllegalStateException,
                "expected IllegalStateException but got: ${thrown::class.qualifiedName}: ${thrown.message}",
            )
        } finally {
            clearIsolatedDataDir()
        }
    }

    @Test
    fun nonDirectBufferIsAutoConvertedAndRejectsGarbageInput() {
        val dataDir = newIsolatedDataDir()
        try {
            // 普通 Heap ByteBuffer
            val heapBuffer = ByteBuffer.wrap("<html><body>heap buffer test</body></html>".toByteArray())
            val thrown = runCatching { parseGalleryList(heapBuffer) }.exceptionOrNull()
            assertTrue(thrown != null, "garbage input must fail")
            assertTrue(
                thrown !is UnsatisfiedLinkError,
                "failure must NOT be an UnsatisfiedLinkError: $thrown",
            )
            assertTrue(
                thrown is IllegalStateException,
                "expected IllegalStateException but got: ${thrown::class.qualifiedName}: ${thrown.message}",
            )
        } finally {
            clearIsolatedDataDir()
        }
    }
}
