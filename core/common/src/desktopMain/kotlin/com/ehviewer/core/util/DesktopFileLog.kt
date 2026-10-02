package com.ehviewer.core.util

import okio.FileSystem
import okio.Path
import okio.buffer

// 日志落盘的纯逻辑：追加写入 + 超 1MB 轮转（保留一代 .log.1）
object DesktopFileLog {
    const val MAX_BYTES = 1L * 1024 * 1024

    fun rotateIfNeeded(file: Path, fs: FileSystem = FileSystem.SYSTEM) {
        if (!fs.exists(file)) return
        if ((fs.metadataOrNull(file)?.size ?: 0L) < MAX_BYTES) return
        val rolled = file.parent!! / "${file.name}.1"
        runCatching { fs.delete(rolled) }
        runCatching { fs.atomicMove(file, rolled) }
    }

    fun append(file: Path, line: String, fs: FileSystem = FileSystem.SYSTEM) {
        file.parent?.let { runCatching { fs.createDirectories(it) } }
        rotateIfNeeded(file, fs)
        fs.appendingSink(file, mustExist = false).buffer().use { sink ->
            sink.writeUtf8(line + "\n")
        }
    }
}
