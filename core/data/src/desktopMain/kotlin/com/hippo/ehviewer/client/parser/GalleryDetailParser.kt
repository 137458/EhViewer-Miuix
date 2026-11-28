package com.hippo.ehviewer.client.parser

import com.ehviewer.core.database.client.unmarshalParsingAs
import com.ehviewer.core.model.GalleryCommentList
import com.ehviewer.core.model.GalleryDetail
import com.ehviewer.core.model.GalleryPreviewList
import java.nio.ByteBuffer
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.CborArray

// 桌面壳：HTML 详情页解析执行体在 Rust 库（JNI 符号绑定本类名，与 Android 侧一致），
// 解析结果经 CBOR unmarshal 为共享模型。app 模块委托本实现。
object GalleryDetailParser {
    @Serializable
    @CborArray
    class Result(val detail: GalleryDetail, val event: String?)

    fun parse(body: ByteBuffer): Result {
        ensureRustBindings()
        return unmarshalParsingAs(body, ::nativeParse)
    }

    fun parseComments(body: ByteBuffer): GalleryCommentList {
        ensureRustBindings()
        return unmarshalParsingAs(body, ::nativeParseComments)
    }

    fun parsePreviews(body: ByteBuffer): GalleryPreviewList {
        ensureRustBindings()
        return unmarshalParsingAs(body, ::nativeParsePreviews)
    }

    // 绑定不可用时抛受控异常：调用方按「解析器不可用」处理，
    // 不能让 UnsatisfiedLinkError 之类的 Error 逃逸到上层
    private fun ensureRustBindings() {
        if (!ensureRustLoaded()) {
            throw IllegalStateException("Rust bindings unavailable: ${rustLoadError() ?: "unknown error"}")
        }
    }

    private external fun nativeParse(body: ByteBuffer, size: Int = body.limit()): Int
    private external fun nativeParseComments(body: ByteBuffer, size: Int = body.limit()): Int
    private external fun nativeParsePreviews(body: ByteBuffer, size: Int = body.limit()): Int
}
