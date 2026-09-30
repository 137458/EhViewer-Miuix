package com.ehviewer.core.util

actual fun platformLog(tag: String, priority: LogPriority, message: String) {
    System.err.println("${priority.name.first()}/$tag: $message")
}
