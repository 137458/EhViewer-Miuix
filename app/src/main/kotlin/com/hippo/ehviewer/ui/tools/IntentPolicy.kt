package com.hippo.ehviewer.ui.tools

/**
 * 外部可触达 intent 的准入判定。
 *
 * MainActivity 的 action 会收到任意第三方 intent：
 * - 内部控件类 action（下载页跳转、安装更新）必须携带进程内 nonce，否则拒绝；
 * - `file://` URI 不得指向应用私有目录（dataDir/filesDir/cacheDir），否则外部可借阅读器读取私有数据。
 */
object IntentPolicy {
    /** action 是否需要进程内令牌。 */
    fun requiresInternalToken(action: String?, restrictedActions: Collection<String>): Boolean = action != null && restrictedActions.contains(action)

    /** 需要令牌的 action 必须逐字匹配；其余 action 直接放行。 */
    fun isInternalIntentTrusted(
        action: String?,
        extraToken: String?,
        expectedToken: String,
        restrictedActions: Collection<String>,
    ): Boolean = !requiresInternalToken(action, restrictedActions) || extraToken == expectedToken

    /** path 是否等于或在任一受保护目录内（按路径段边界比较，避免 `/a/b` 误伤 `/a/bc`）。 */
    fun isInsideAnyDir(path: String?, dirs: Collection<String>): Boolean {
        if (path.isNullOrEmpty()) return false
        val normalized = normalize(path)
        if (normalized.isEmpty()) return false
        return dirs.any { dir ->
            if (dir.isEmpty()) return@any false
            val base = normalize(dir)
            base.isNotEmpty() && (normalized == base || normalized.startsWith("$base/"))
        }
    }

    /** 归一化：统一分隔符并消解 `.` / `..`，避免用相对路径越出受保护目录。 */
    private fun normalize(path: String): String {
        val segments = ArrayDeque<String>()
        for (segment in path.replace('\\', '/').split('/')) {
            when (segment) {
                "", "." -> Unit
                ".." -> segments.removeLastOrNull()
                else -> segments.addLast(segment)
            }
        }
        return segments.joinToString("/")
    }
}
