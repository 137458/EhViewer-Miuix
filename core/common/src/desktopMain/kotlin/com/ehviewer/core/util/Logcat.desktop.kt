package com.ehviewer.core.util

actual fun platformLog(tag: String, priority: LogPriority, message: () -> String) {
    val text = "${priority.name.first()}/$tag: ${message()}"
    System.err.println(text)
    // 诊断落盘：桌面端 stdout 用户不可见，追加到数据区日志文件（与 DesktopDirs 同源路径规则）
    // 任何 IO 失败静默忽略——日志永远不能影响应用本身
    DesktopFileLog.defaultFile()?.let { file -> runCatching { DesktopFileLog.append(file, text) } }
}
