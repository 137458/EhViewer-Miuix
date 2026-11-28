package com.hippo.ehviewer.util

/**
 * App 内部 Intent / PendingIntent 的进程级标记。
 *
 * token 每次进程启动随机生成，外部应用无法得知，因此接收方可以用它拒掉伪造的广播与 PendingIntent。
 */
object InternalIntentGuard {
    const val EXTRA_TOKEN = "eh_internal_token"
    val token: String = java.util.UUID.randomUUID().toString()
}
