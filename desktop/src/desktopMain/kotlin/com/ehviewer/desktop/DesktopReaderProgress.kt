package com.ehviewer.desktop

import java.util.concurrent.ConcurrentHashMap

// 阅读器会话内页码记忆：库 ↔ 阅读器往返不丢页码（内存级，故意不持久化——持久化需数据库迁移，见 human_todo）
object DesktopReaderProgress {
    private val pages = ConcurrentHashMap<Long, Int>()

    fun save(gid: Long, page: Int) {
        if (page >= 1) pages[gid] = page
    }

    fun restore(gid: Long, totalPages: Int): Int {
        if (totalPages < 1) return 1
        return (pages[gid] ?: 1).coerceIn(1, totalPages)
    }
}
