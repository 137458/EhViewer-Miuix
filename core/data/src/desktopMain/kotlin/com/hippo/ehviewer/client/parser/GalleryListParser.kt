package com.hippo.ehviewer.client.parser

import com.ehviewer.core.DesktopDirs
import com.ehviewer.core.model.BaseGalleryInfo
import io.ktor.util.moveToByteArray
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray

// HTML 列表解析的执行体在 Rust 库（ehviewer_rust，jvm feature），
// JNI 符号由 ffi/jvm.rs 的 jni_fn 声明为 GalleryListParserKt——本文件类名/包名必须与之逐字一致。
private const val NATIVE_RESOURCE = "native/ehviewer_rust.dll"
private const val NATIVE_LIBRARY_NAME = "ehviewer_rust"
private const val DLL_OVERRIDE = "EHVIEWER_RUST_DLL"

// 原生库只尝试加载一次：并发调用串行化，失败结果同样缓存，避免每次解析都重复解压/写盘
private object RustLibraryLoader {
    private val loaded = AtomicBoolean(false)

    @Volatile
    private var loadError: String? = null

    val available: Boolean get() = loaded.get()

    val error: String? get() = loadError

    @Synchronized
    fun load(): Boolean {
        if (loaded.get()) return true
        val errors = mutableListOf<String>()
        val success = loadExplicitOverride(errors) || loadFromClasspath(errors) || loadFromLibraryPath(errors)
        if (success) {
            loadError = null
            loaded.set(true)
        } else {
            loadError = errors.joinToString(" | ")
        }
        return success
    }

    // 显式指定的 dll（开发/自定义分发）优先；生产不依赖相对工作目录的 cargo 产物
    private fun loadExplicitOverride(errors: MutableList<String>): Boolean {
        val configured = System.getProperty(DLL_OVERRIDE)?.takeIf { it.isNotBlank() }
            ?: System.getenv(DLL_OVERRIDE)?.takeIf { it.isNotBlank() }
        if (configured == null) {
            errors += "override: $DLL_OVERRIDE not set"
            return false
        }
        val file = File(configured)
        if (!file.isFile) {
            errors += "override: ${file.absolutePath} does not exist"
            return false
        }
        return runCatching { System.load(file.canonicalPath) }
            .onFailure { errors += "override: ${it.message}" }
            .isSuccess
    }

    // 类路径资源按内容 SHA-256 命名落到用户私有目录：不落 %TEMP%，同名文件必然同内容，可直接复用
    private fun loadFromClasspath(errors: MutableList<String>): Boolean {
        val bytes = runCatching {
            val loader = Thread.currentThread().contextClassLoader ?: GalleryListParserKtProbe::class.java.classLoader
            val stream = loader?.getResourceAsStream(NATIVE_RESOURCE)
                ?: GalleryListParserKtProbe::class.java.getResourceAsStream("/$NATIVE_RESOURCE")
            stream?.use { it.readBytes() }
        }.getOrElse {
            errors += "classpath: ${it.message}"
            null
        }
        if (bytes == null) {
            errors += "classpath: $NATIVE_RESOURCE not present"
            return false
        }
        return runCatching {
            val digest = sha256(bytes)
            val target = File(nativeDir(), "ehviewer_rust-$digest.dll")
            if (!target.isFile || sha256(target.readBytes()) != digest) {
                writeAtomically(target, bytes)
            }
            System.load(target.absolutePath)
        }.onFailure { errors += "classpath: ${it.message}" }.isSuccess
    }

    private fun loadFromLibraryPath(errors: MutableList<String>): Boolean = runCatching { System.loadLibrary(NATIVE_LIBRARY_NAME) }
        .onFailure { errors += "loadLibrary: ${it.message}" }
        .isSuccess

    private fun nativeDir(): File {
        val dir = File(DesktopDirs.filesDir.toString(), "native")
        if (!dir.isDirectory && !dir.mkdirs() && !dir.isDirectory) {
            throw IOException("cannot create native directory: ${dir.absolutePath}")
        }
        return dir
    }

    private fun writeAtomically(target: File, bytes: ByteArray) {
        val temp = File(target.parentFile, "${target.name}.${System.nanoTime()}.tmp")
        try {
            temp.writeBytes(bytes)
            val source = temp.toPath()
            val destination = target.toPath()
            runCatching {
                Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            }.onFailure {
                // Windows 上目标可能被已加载的旧句柄占用：删除后重试
                target.delete()
                Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            temp.delete()
        }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }
}

internal fun ensureRustLoaded(): Boolean = RustLibraryLoader.load()

// 诊断探针
object GalleryListParserKtProbe {
    val resAvailable: Boolean
        get() = (Thread.currentThread().contextClassLoader ?: javaClass.classLoader)
            ?.getResource(NATIVE_RESOURCE) != null ||
            javaClass.getResource("/$NATIVE_RESOURCE") != null
    val loadError: String? get() = RustLibraryLoader.error
}

// 供测试判断原生绑定是否可用（CI/Linux 无 dll 时跳过相关用例）
val rustGalleryBindingsAvailable: Boolean get() = RustLibraryLoader.available

// 加载失败原因（用于受控异常消息）
internal fun rustLoadError(): String? = RustLibraryLoader.error

@Serializable
data class GalleryListResult(
    val prev: String?,
    val next: String?,
    val galleryInfoList: ArrayList<BaseGalleryInfo>,
)

external fun parseGalleryInfoList(body: ByteBuffer, size: Int = body.limit()): Int

fun parseGalleryList(body: ByteBuffer): GalleryListResult {
    check(ensureRustLoaded()) {
        "Rust bindings unavailable: ${rustLoadError() ?: "unknown error"}"
    }
    val directBody = if (body.isDirect && body.position() == 0) {
        body
    } else {
        val pos = body.position()
        val limit = body.limit()
        val direct = ByteBuffer.allocateDirect(limit - pos)
        direct.put(body.duplicate())
        direct.flip()
        direct
    }
    check(directBody.isDirect && directBody.position() == 0) {
        "native parser requires a direct ByteBuffer at position 0"
    }
    val cborSize = try {
        parseGalleryInfoList(directBody, directBody.limit())
    } catch (e: Exception) {
        throw IllegalStateException("Can't parse gallery list", e)
    }
    // native 返回结束位置：越界或负数说明结果不可信，必须在此拦截而不是越界读取
    check(cborSize in 0..directBody.capacity()) {
        "native parser returned invalid CBOR size $cborSize (capacity=${directBody.capacity()})"
    }
    directBody.limit(cborSize)
    val array = directBody.moveToByteArray()
    return try {
        Cbor.decodeFromByteArray<GalleryListResult>(array)
    } catch (e: Exception) {
        throw IllegalStateException("Can't parse gallery list", e)
    }
}
