package com.ehviewer.core.util

import logcat.LogPriority as LibPriority
import logcat.logcat

actual fun platformLog(tag: String, priority: LogPriority, message: () -> String) {
    logcat(tag, priority.toLibPriority(), message)
}

private fun LogPriority.toLibPriority(): LibPriority = when (this) {
    LogPriority.VERBOSE -> LibPriority.VERBOSE
    LogPriority.DEBUG -> LibPriority.DEBUG
    LogPriority.INFO -> LibPriority.INFO
    LogPriority.WARN -> LibPriority.WARN
    LogPriority.ERROR -> LibPriority.ERROR
    LogPriority.ASSERT -> LibPriority.ASSERT
}
