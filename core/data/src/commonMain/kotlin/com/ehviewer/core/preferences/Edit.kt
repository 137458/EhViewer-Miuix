package com.ehviewer.core.preferences

import com.ehviewer.core.mainthread.checkNotMainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun <P : DataStorePreferences> P.edit(blocking: Boolean = false, block: P.(MutableSnapshot) -> Unit) {
    if (blocking) {
        checkNotMainThread()
        runBlocking {
            withMutableSnapshot { block(it) }
        }
    } else {
        prefScope.launch {
            withMutableSnapshot { block(it) }
        }
    }
}

// 写入必须按提交顺序串行执行：并行调度下后提交的 edit 可能先落盘，
// 最终磁盘状态与调用顺序不一致
internal val prefScope = CoroutineScope(Dispatchers.IO.limitedParallelism(1) + SupervisorJob())
