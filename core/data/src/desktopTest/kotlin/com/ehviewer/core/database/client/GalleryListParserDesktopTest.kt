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
        // dll 缺失的环境（CI/Linux）跳过本用例
        if (!rustGalleryBindingsAvailable) return
        val dataDir = newIsolatedDataDir()
        try {
            val garbage = ByteBuffer.wrap("<html>not a gallery list</html>".toByteArray())
            val thrown = runCatching { parseGalleryList(garbage) }.exceptionOrNull()
            assertTrue(thrown != null, "garbage input must fail at the Rust parse layer")
            // 失败须来自解析语义（IllegalStateException 包装），而非 UnsatisfiedLinkError
            assertTrue(
                thrown !is UnsatisfiedLinkError,
                "failure must be a parse error, not a binding error: $thrown",
            )
        } finally {
            clearIsolatedDataDir()
        }
    }
}
