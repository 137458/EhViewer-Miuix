package com.hippo.ehviewer.client.parser

import com.ehviewer.core.model.BaseGalleryInfo
import io.ktor.util.moveToByteArray
import java.nio.ByteBuffer
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray

// HTML 列表解析的执行体在 Rust 库（ehviewer_rust，jvm feature），
// JNI 符号由 ffi/jvm.rs 的 jni_fn 声明为 GalleryListParserKt——本文件类名/包名必须与之逐字一致。
private val rustLibrariesLoaded = runCatching {
    System.loadLibrary("ehviewer_rust")
}.recoverCatching {
    // 开发/测试环境：dll 不在 java.library.path 时按候选路径加载
    val path = candidateDllPaths().firstOrNull { java.io.File(it).exists() }
        ?: throw IllegalStateException("ehviewer_rust library not found (set EHVIEWER_RUST_DLL to override)")
    System.load(path)
}.isSuccess

// 供测试判断原生绑定是否可用（CI/Linux 无 dll 时跳过相关用例）
val rustGalleryBindingsAvailable: Boolean get() = rustLibrariesLoaded

// dll 候选：环境变量显式指定 > 模块工作目录的 cargo 产物（desktopTest 的 cwd 为模块目录）
private fun candidateDllPaths(): List<String> = buildList {
    System.getenv("EHVIEWER_RUST_DLL")?.let(::add)
    add("../../app/src/main/rust/target-desk/x86_64-pc-windows-gnu/release/ehviewer_rust.dll")
    add("../../app/src/main/rust/target/x86_64-pc-windows-gnu/release/ehviewer_rust.dll")
}

data class GalleryListResult(
    val prev: String?,
    val next: String?,
    val galleryInfoList: ArrayList<BaseGalleryInfo>,
)

external fun parseGalleryInfoList(body: ByteBuffer, size: Int = body.limit()): Int

fun parseGalleryList(body: ByteBuffer): GalleryListResult = try {
    val cborSize = parseGalleryInfoList(body, body.limit())
    body.limit(cborSize)
    val array = body.moveToByteArray()
    Cbor.decodeFromByteArray<GalleryListResult>(array)
} catch (e: Exception) {
    throw IllegalStateException("Can't parse gallery list", e)
}
