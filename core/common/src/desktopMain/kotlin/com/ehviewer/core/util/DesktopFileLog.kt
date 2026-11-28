package com.ehviewer.core.util

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer

// append 与 rotate 必须成对串行：并发写入时各自先看到超限再轮转会互相覆盖，丢掉整份日志
private val logLock = Any()

// 日志落盘的纯逻辑：追加写入 + 超 1MB 轮转（保留一代 .log.1）
object DesktopFileLog {
    const val MAX_BYTES = 1L * 1024 * 1024

    // 默认日志文件：与 core:data DesktopDirs 相同的根目录规则
    // （ehviewer.data.dir 属性 → %APPDATA% → $XDG_STATE_HOME → ~/.ehviewer）；core:common 不依赖 core:data，逻辑在此锚定同步
    fun defaultFile(): Path? = runCatching {
        val base = resolveLogRootDir(
            dataDirProperty = System.getProperty("ehviewer.data.dir"),
            appData = System.getenv("APPDATA"),
            xdgStateHome = System.getenv("XDG_STATE_HOME"),
            userHome = System.getProperty("user.home"),
        ) ?: return@runCatching null
        "$base/EhViewer/files/logs/ehviewer.log".toPath()
    }.getOrNull()

    fun rotateIfNeeded(file: Path, fs: FileSystem = FileSystem.SYSTEM) {
        if (!fs.exists(file)) return
        if ((fs.metadataOrNull(file)?.size ?: 0L) < MAX_BYTES) return
        val rolled = rolledLogPath(file.parent, file.name)
        runCatching { fs.delete(rolled) }
        runCatching { fs.atomicMove(file, rolled) }
    }

    fun append(file: Path, line: String, fs: FileSystem = FileSystem.SYSTEM) {
        withLogLock {
            file.parent?.let { runCatching { fs.createDirectories(it) } }
            rotateIfNeeded(file, fs)
            fs.appendingSink(file, mustExist = false).buffer().use { sink ->
                sink.writeUtf8(line + "\n")
            }
        }
    }
}

internal fun <T> withLogLock(block: () -> T): T = synchronized(logLock) { block() }

// 轮转文件名：parent 为 null（根路径等退化情形）时退回同目录相对名
internal fun rolledLogPath(parent: Path?, name: String): Path = parent?.div("$name.1") ?: "$name.1".toPath()

// 日志根目录推导（纯函数，便于测试优先级与路径规范化）
internal fun resolveLogRootDir(
    dataDirProperty: String?,
    appData: String?,
    xdgStateHome: String?,
    userHome: String?,
): String? = normalizeRootDir(dataDirProperty)
    ?: normalizeRootDir(appData)
    ?: normalizeRootDir(xdgStateHome)
    ?: normalizeRootDir(userHome)?.let { "$it/.ehviewer" }

private fun normalizeRootDir(value: String?): String? = value
    ?.takeIf { it.isNotBlank() }
    ?.replace('\\', '/')
    ?.trimEnd('/')
    ?.takeIf { it.isNotEmpty() }
