package com.hippo.ehviewer.client.parser

import java.nio.ByteBuffer
import org.junit.Assume
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Rust 详情页解析的桌面端冒烟：加载真实详情页 HTML 样本（资源文件），
// 经 nativeParse → CBOR unmarshal 还原 GalleryDetail。
// 前提：测试环境可加载 ehviewer_rust 库（Windows 本地）；无 dll 的环境（CI/Linux）经 Assume 跳过。
class GalleryDetailParserDesktopTest {

    private fun assumeRustOrSkip() = Assume.assumeTrue(rustGalleryBindingsAvailable)

    private fun sampleBody(): ByteBuffer {
        val stream = javaClass.classLoader.getResourceAsStream("gdetail_sample.html")
            ?: error("gdetail_sample.html resource missing")
        val html = stream.use { it.readBytes() }
        return ByteBuffer.allocateDirect(html.size).put(html).flip()
    }

    @Test
    fun nativeParseExtractsGalleryDetailFromRealSample() {
        assumeRustOrSkip()
        val result = GalleryDetailParser.parse(sampleBody())
        assertNotNull(result)
        assertNotNull(result.detail.galleryInfo)
        // 真实样本：标题必然非空（详情页核心字段）
        assertTrue(result.detail.galleryInfo.title?.isNotEmpty() == true)
    }

    @Test
    fun commentsAndPreviewsSmoke() {
        assumeRustOrSkip()
        val buffer = sampleBody()
        val comments = GalleryDetailParser.parseComments(buffer)
        assertNotNull(comments)
        val previews = GalleryDetailParser.parsePreviews(sampleBody())
        assertNotNull(previews)
    }
}
