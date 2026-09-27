package com.hippo.ehviewer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.ehviewer.core.ui.util.AdaptiveBreakpoints
import com.ehviewer.core.ui.util.LocalWindowLayout
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.collectAsState

/**
 * 当前生效的缩略图列数配置：横屏下若单独设置过横屏列数则使用之，否则回退竖屏配置。
 *
 * 列数仍会经 [WindowLayout.thumbGridColumns] 按可用宽度补足，本函数只决定「用户配置基准值」。
 */
@Composable
fun collectConfiguredThumbColumns(): Int {
    val isLandscape = LocalWindowLayout.current.isLandscape
    val portrait by Settings.thumbColumns.collectAsState()
    val landscape by Settings.thumbColumnsLand.collectAsState()
    // 下限固定为 1：0 是「未单独设置横屏列数」的哨兵值，必须回退竖屏配置
    return if (isLandscape && landscape in 1..AdaptiveBreakpoints.THUMB_COLUMNS_MAX) landscape else portrait
}
