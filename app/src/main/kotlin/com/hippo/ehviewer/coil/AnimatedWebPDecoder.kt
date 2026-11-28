package com.hippo.ehviewer.coil

import android.content.res.AssetFileDescriptor
import android.os.ParcelFileDescriptor
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.ByteBufferMetadata
import coil3.decode.ContentMetadata
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.gif.isAnimatedWebP
import coil3.request.Options
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import okio.FileSystem

class AnimatedWebPDecoder(private val source: ByteBuffer) : Decoder {
    override suspend fun decode() = DecodeResult(AnimatedWebPDrawable(source).asImage(), false)

    object Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ) = if (DecodeUtils.isAnimatedWebP(result.source.source())) {
            result.source.toByteBufferOrNull()?.let { AnimatedWebPDecoder(it) }
        } else {
            null
        }
    }
}

private fun ImageSource.toByteBufferOrNull(): ByteBuffer? {
    if (fileSystem === FileSystem.SYSTEM) {
        val file = fileOrNull()
        if (file != null) {
            return file.toFile().inputStream().mapReadOnly()
        }
    }
    return when (val metadata = metadata) {
        is ContentMetadata -> metadata.assetFileDescriptor.readContentRange()
        is ByteBufferMetadata -> metadata.byteBuffer
        else -> null
    }
}

private fun FileInputStream.mapReadOnly(): ByteBuffer = channel.use { it.map(FileChannel.MapMode.READ_ONLY, 0, it.size()) }

// AssetFileDescriptor 常常只是容器文件（APK / OBB）里的一段区间：直接 mmap 整份文件会把容器头当成 WebP 数据，
// 也会忽略 declaredLength；UNKNOWN_LENGTH 时长度未知。这里 dup 一份 fd（不动 Coil 持有的原 descriptor），
// 已知区间时只映射 [startOffset, startOffset + declaredLength)，否则按字节流读入堆内 buffer。
private fun AssetFileDescriptor.readContentRange(): ByteBuffer? = ParcelFileDescriptor.dup(parcelFileDescriptor.fileDescriptor).use { dup ->
    FileInputStream(dup.fileDescriptor).use { input ->
        val channel = input.channel
        val start = startOffset
        val declared = declaredLength
        if (declared != AssetFileDescriptor.UNKNOWN_LENGTH && declared > 0L) {
            // FileChannel.map 映射的是文件区间本身，与通道当前位置无关
            try {
                return channel.map(FileChannel.MapMode.READ_ONLY, start, declared)
            } catch (_: IOException) {
                // 管道/不可映射的内容提供者退回按字节流读取
            }
        }
        channel.position(start)
        val bytes = ByteArrayOutputStream()
        val chunk = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(chunk)
            if (read < 0) break
            bytes.write(chunk, 0, read)
        }
        ByteBuffer.wrap(bytes.toByteArray())
    }
}
