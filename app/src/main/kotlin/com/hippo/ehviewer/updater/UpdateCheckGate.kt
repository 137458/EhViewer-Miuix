package com.hippo.ehviewer.updater

/**
 * 自动检查闸门：只在发起网络请求前判定，不参与「忽略此版本」或「是否已经提示过」。
 * [intervalDays] 为 0 表示从不自动检查；[forceCheck] 为用户主动触发，直接放行。
 */
internal object UpdateCheckGate {
    private const val SECONDS_PER_DAY = 86_400L

    fun shouldCheck(
        forceCheck: Boolean,
        lastCheckEpochSeconds: Long,
        intervalDays: Int,
        nowEpochSeconds: Long,
    ): Boolean {
        if (forceCheck) return true
        if (intervalDays == 0) return false
        return nowEpochSeconds > lastCheckEpochSeconds + intervalDays * SECONDS_PER_DAY
    }
}
