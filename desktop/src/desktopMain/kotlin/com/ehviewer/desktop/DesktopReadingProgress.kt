package com.ehviewer.desktop

// 阅读进度持久化（gid → 最后阅读页码），跨会话续读。
// 采用轻量 "gid:page" 文本编解码，避免 desktop 模块引入 kotlinx-serialization 扩展的可见性问题。
object DesktopReadingProgress {
    const val MAX_ENTRIES = 50

    fun encode(progress: Map<Long, Int>): String = progress.entries.joinToString(",") { "${it.key}:${it.value}" }

    fun decode(raw: String?): Map<Long, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(',')
            .mapNotNull { entry ->
                val parts = entry.split(':')
                if (parts.size == 2) {
                    parts[0].toLongOrNull()?.let { gid ->
                        parts[1].toIntOrNull()?.let { page -> gid to page }
                    }
                } else {
                    null
                }
            }
            .take(MAX_ENTRIES)
            .toMap()
    }

    // LRU 语义：更新/新增的条目移到最前，配合 decode 的 take(MAX_ENTRIES) 淘汰最旧——否则写满后新画廊进度会被截断丢弃
    fun update(progress: Map<Long, Int>, gid: Long, page: Int): Map<Long, Int> = mapOf(gid to page.coerceAtLeast(1)) + (progress - gid)
}
