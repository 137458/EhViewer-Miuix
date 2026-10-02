package com.ehviewer.core.util

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer

actual fun platformLog(tag: String, priority: LogPriority, message: () -> String) {
    val text = "${priority.name.first()}/$tag: ${message()}"
    System.err.println(text)
    // 诊断落盘：桌面端 stdout 用户不可见，追加到数据区日志文件（与 DesktopDirs 同源路径规则）
    // 任何 IO 失败静默忽略——日志永远不能影响应用本身
    logFile?.let { file -> runCatching { DesktopFileLog.append(file, text) } }
}

// 与 core:data DesktopDirs 相同的根目录规则（ehviewer.data.dir 属性 → %APPDATA% → ~/.ehviewer）；
// core:common 不依赖 core:data，路径逻辑在此重复并以注释锚定同步
private val logFile: Path? by lazy {
    runCatching {
        val base = System.getProperty("ehviewer.data.dir")?.takeIf { it.isNotBlank() }?.replace('\\', '/')
            ?: System.getenv("APPDATA")?.replace('\\', '/')
            ?: (System.getProperty("user.home") + "/.ehviewer")
        "$base/EhViewer/files/logs/ehviewer.log".toPath()
    }.getOrNull()
}
