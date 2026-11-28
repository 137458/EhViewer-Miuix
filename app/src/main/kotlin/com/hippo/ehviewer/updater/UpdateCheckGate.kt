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
        // 0 表示关闭自动检查，负数（脏配置）同样不能放大成"每次都检查"
        if (intervalDays <= 0) return false
        // 时钟回拨（现在早于上次检查）时不做自动检查，否则时间跳变会反复触发网络请求
        if (nowEpochSeconds < lastCheckEpochSeconds) return false
        return nowEpochSeconds > lastCheckEpochSeconds + intervalDays * SECONDS_PER_DAY
    }
}
