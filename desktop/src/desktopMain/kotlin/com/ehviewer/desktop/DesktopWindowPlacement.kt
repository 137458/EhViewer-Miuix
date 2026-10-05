package com.ehviewer.desktop

import java.awt.Rectangle

// 窗口位置保存前的可见性校验：任一屏幕包含该坐标即可恢复，避免外接显示器拔除后窗口恢复到屏幕外不可见
fun isPositionWithinScreens(x: Int, y: Int, screens: List<Rectangle>): Boolean = screens.any { it.contains(x, y) }

object DesktopWindowPlacement {

    // 屏幕工作区（已扣除任务栏，dp 尺度）
    data class WorkArea(val width: Int, val height: Int)

    // 恢复的窗口尺寸钳入工作区：高 DPI 下超屏记忆尺寸会被 AWT 钳到最小尺寸，
    // 造成 windowState 与实际窗口脱钩、内容按旧尺寸布局被裁切（实测 density=1.75 时 awt 落到 600x500）
    fun clampWindowSizeToWorkArea(
        widthDp: Int,
        heightDp: Int,
        minWidthDp: Int,
        minHeightDp: Int,
        workArea: WorkArea?,
    ): Pair<Int, Int> {
        if (workArea == null) return widthDp to heightDp
        val w = widthDp.coerceAtMost(workArea.width).coerceAtLeast(minWidthDp)
        val h = heightDp.coerceAtMost(workArea.height).coerceAtLeast(minHeightDp)
        return w to h
    }
}
