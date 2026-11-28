package com.ehviewer.core.files

import android.content.ContentResolver
import android.net.Uri
import androidx.core.net.toUri
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import okio.Path
import okio.Path.Companion.toPath

fun Path.openFileDescriptor(mode: String) = PlatformSystemFileSystem.openFileDescriptor(this, mode)

actual inline fun <T> Path.read(f: Source.() -> T) = PlatformSystemFileSystem.rawSource(this).buffered().use(f)

actual inline fun <T> Path.write(f: Sink.() -> T) = PlatformSystemFileSystem.rawSink(this).buffered().use(f)

fun Path.toUri(): Uri {
    val str = toString()
    if (str.startsWith('/')) {
        return toFile().toUri()
    }

    val uri = str.replaceFirst("content:/", "content://").toUri()
    val path = requireNotNull(uri.encodedPath) { "Invalid path: $str" }
    val paths = path.split('/').dropWhile { it.isEmpty() }
    val documentId = rebuildTreeDocumentId(paths) { Uri.decode(it) } ?: return uri
    return uri.buildUpon().apply {
        path(null)
        // 原 URI 的查询串/片段属于原 document，重建后的目标文档不该继承，否则会被当成路径文本编码
        query(null)
        fragment(null)
        repeat(3) { i ->
            appendEncodedPath(paths[i])
        }
        appendPath(documentId)
    }.build()
}

fun Uri.toOkioPath() = if (scheme == ContentResolver.SCHEME_FILE) {
    requireNotNull(path) { "Invalid URI: $this" }
} else {
    toString()
}.toPath()
