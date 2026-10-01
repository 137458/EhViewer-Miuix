package com.hippo.ehviewer.client.parser

import com.ehviewer.core.model.BaseGalleryInfo
import io.ktor.util.moveToByteArray
import java.nio.ByteBuffer
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray

// HTML 列表解析的执行体在 Rust 库（ehviewer_rust，jvm feature），
// JNI 符号由 ffi/jvm.rs 的 jni_fn 声明为 GalleryListParserKt——本文件类名/包名必须与之逐字一致。
private var rustLibrariesLoaded = false
    get() {
        if (!field) {
            field = ensureRustLoaded()
        }
        return field
    }

private var lastLoadError: String? = null

private fun ensureRustLoaded(): Boolean {
    val errors = mutableListOf<String>()
    runCatching {
        System.loadLibrary("ehviewer_rust")
        return true
    }.onFailure { errors += "loadLibrary: ${it.message}" }
    runCatching {
        // 类路径资源（Gradle 构建/分发时从 Rust 产物复制）
        Thread.currentThread().contextClassLoader.getResourceAsStream("native/ehviewer_rust.dll")?.use { input ->
            val tmp = java.io.File.createTempFile("ehviewer_rust", ".dll")
            input.copyTo(tmp.outputStream())
            tmp.deleteOnExit()
            System.load(tmp.absolutePath)
            return true
        }
        errors += "resource: not on classpath"
    }.onFailure { errors += "resource: ${it.message}" }
    runCatching {
        // 兜底：环境变量/相对路径候选
        val path = candidateDllPaths().firstOrNull { java.io.File(it).exists() }
            ?: throw IllegalStateException("no candidate exists")
        System.load(path)
        return true
    }.onFailure { errors += "candidates: ${it.message}" }
    lastLoadError = errors.joinToString(" | ")
    return false
}

// 诊断探针
object GalleryListParserKtProbe {
    val resAvailable: Boolean
        get() = Thread.currentThread().contextClassLoader
            ?.getResource("native/ehviewer_rust.dll") != null
    val loadError: String? get() = lastLoadError
}

// 供测试判断原生绑定是否可用（CI/Linux 无 dll 时跳过相关用例）
val rustGalleryBindingsAvailable: Boolean get() = rustLibrariesLoaded

// dll 候选：环境变量/系统属性显式指定 > 各工作目录下的 cargo 产物
private fun candidateDllPaths(): List<String> = buildList {
    System.getProperty("EHVIEWER_RUST_DLL")?.let(::add)
    System.getenv("EHVIEWER_RUST_DLL")?.let(::add)
    add("../../app/src/main/rust/target-desk/x86_64-pc-windows-gnu/release/ehviewer_rust.dll")
    add("../../app/src/main/rust/target/x86_64-pc-windows-gnu/release/ehviewer_rust.dll")
    add("app/src/main/rust/target-desk/x86_64-pc-windows-gnu/release/ehviewer_rust.dll")
    add("app/src/main/rust/target/x86_64-pc-windows-gnu/release/ehviewer_rust.dll")
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
