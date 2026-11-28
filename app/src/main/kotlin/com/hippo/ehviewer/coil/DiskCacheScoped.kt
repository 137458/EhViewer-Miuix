package com.hippo.ehviewer.coil

import coil3.disk.DiskCache

inline fun DiskCache.edit(key: String, crossinline block: DiskCache.Editor.() -> Unit): Boolean {
    val editor = openEditor(key) ?: return false
    editor.runCatching {
        block(this)
    }.onFailure {
        // abort 自身可能抛 IO 异常，不能让它覆盖掉真正的失败原因
        runCatching { editor.abort() }
        throw it
    }.onSuccess {
        editor.commit()
    }
    return true
}

suspend inline fun DiskCache.suspendEdit(key: String, crossinline block: suspend DiskCache.Editor.() -> Unit): Boolean {
    val editor = openEditor(key) ?: return false
    editor.runCatching {
        block(this)
    }.onFailure {
        // abort 自身可能抛 IO 异常，不能让它覆盖掉真正的失败原因
        runCatching { editor.abort() }
        throw it
    }.onSuccess {
        editor.commit()
    }
    return true
}

inline fun <T> DiskCache.read(key: String, block: DiskCache.Snapshot.() -> T): T? = openSnapshot(key)?.use(block)
