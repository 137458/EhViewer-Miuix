package com.ehviewer.core.util

// 优先级枚举与平台无关的日志出口：logcat 库为 Android 专属，
// 桌面目标由各平台 actual 提供实现。
enum class LogPriority {
    VERBOSE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
    ASSERT,
}

expect fun platformLog(tag: String, priority: LogPriority, message: String)

inline fun logcat(tag: String, priority: LogPriority = LogPriority.DEBUG, crossinline message: () -> String) {
    platformLog(tag, priority, message())
}

inline fun Any.logcat(priority: LogPriority = LogPriority.DEBUG, crossinline message: () -> String) {
    platformLog(this::class.simpleName ?: "Unknown", priority, message())
}

fun logcat(tag: String, throwable: Throwable) {
    platformLog(tag, LogPriority.ERROR, throwable.stackTraceToString())
}

fun Any.logcat(throwable: Throwable) {
    platformLog(this::class.simpleName ?: "Unknown", LogPriority.ERROR, throwable.stackTraceToString())
}
