package com.ehviewer.desktop

import kotlin.math.roundToInt

object DesktopZoomController {
    const val MIN_SCALE = 0.5f
    const val MAX_SCALE = 4.0f
    const val DEFAULT_STEP = 0.25f

    /**
     * 放大缩放比例，步进并限制最大缩放
     */
    fun zoomIn(
        currentScale: Float,
        step: Float = DEFAULT_STEP,
        maxScale: Float = MAX_SCALE,
    ): Float {
        val next = ((currentScale + step) * 100).roundToInt() / 100f
        return next.coerceAtMost(maxScale)
    }

    /**
     * 缩小缩放比例，步进并限制最小缩放
     */
    fun zoomOut(
        currentScale: Float,
        step: Float = DEFAULT_STEP,
        minScale: Float = MIN_SCALE,
    ): Float {
        val next = ((currentScale - step) * 100).roundToInt() / 100f
        return next.coerceAtLeast(minScale)
    }

    /**
     * 重置缩放比例为 1.0 (100%)
     */
    fun resetZoom(): Float = 1.0f

    /**
     * 双击在适应窗口与两倍放大之间双向切换
     */
    fun toggleFitZoom(
        currentScale: Float,
        fitScale: Float = 1.0f,
        zoomedScale: Float = 2.0f,
    ): Float = if (currentScale == fitScale) zoomedScale else fitScale

    /**
     * 格式化缩放比例为百分比字符串（例如 "100%", "150%"）
     */
    fun formatZoomPercentage(scale: Float): String {
        val percent = (scale * 100).roundToInt()
        return "$percent%"
    }
}
