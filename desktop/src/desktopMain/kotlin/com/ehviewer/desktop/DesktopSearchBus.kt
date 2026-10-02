package com.ehviewer.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// 跨窗口搜索总线：详情窗口点击标签等场景向主库发起搜索，主库消费后清空（一次性语义）
object DesktopSearchBus {
    var pendingQuery by mutableStateOf<String?>(null)
        private set

    fun request(query: String) {
        val q = query.trim()
        if (q.isNotEmpty()) pendingQuery = q
    }

    fun consume() {
        pendingQuery = null
    }
}
