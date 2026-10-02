package com.ehviewer.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// 主库批量多选状态：Ctrl+点击 toggle、selectOnly 单选收敛、行删除时同步移除、切 Tab/刷新时 clear
class DesktopMultiSelection {
    var gids by mutableStateOf<Set<Long>>(emptySet())
        private set

    val isActive: Boolean get() = gids.isNotEmpty()

    fun toggle(gid: Long) {
        gids = if (gid in gids) gids - gid else gids + gid
    }

    fun selectOnly(gid: Long) {
        gids = setOf(gid)
    }

    fun remove(gid: Long) {
        gids = gids - gid
    }

    fun clear() {
        gids = emptySet()
    }
}
