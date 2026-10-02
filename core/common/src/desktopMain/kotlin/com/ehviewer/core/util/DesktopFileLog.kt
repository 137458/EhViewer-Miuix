package com.ehviewer.core.util

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer

// 日志落盘的纯逻辑：追加写入 + 超 1MB 轮转（保留一代 .log.1）
object DesktopFileLog {
    const val MAX_BYTES = 1L * 1024 * 1024

    // 默认日志文件：与 core:data DesktopDirs 相同的根目录规则
    // （ehviewer.data.dir 属性 → %APPDATA% → ~/.ehviewer）；core:common 不依赖 core:data，逻辑在此锚定同步
    fun defaultFile(): Path? = runCatching {
        val base = System.getProperty("ehviewer.data.dir")?.takeIf { it.isNotBlank() }?.replace('\\', '/')
            ?: System.getenv("APPDATA")?.replace('\\', '/')
            ?: (System.getProperty("user.home") + "/.ehviewer")
        "$base/EhViewer/files/logs/ehviewer.log".toPath()
    }.getOrNull()

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
