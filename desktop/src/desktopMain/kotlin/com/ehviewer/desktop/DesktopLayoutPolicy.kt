package com.ehviewer.desktop

// 桌面大屏布局策略（对齐 MIUIX 规范第 4 节，参照 PixEz MIUIX 桌面端实践）：
// 600dp 宽窄断点、单列内容限宽 760dp 居中、网格卡片最小 180dp 自适应列数、
// 窗口最小尺寸与断点一致（桌面恒为宽屏形态）。
object DesktopLayoutPolicy {
    const val WIDE_SCREEN_MIN_WIDTH_DP = 600
    const val CONTENT_MAX_WIDTH_DP = 760
    const val GRID_CARD_MIN_WIDTH_DP = 180
    const val LIBRARY_LIST_WIDTH_DP = 340
    const val WINDOW_MIN_WIDTH_DP = 600
    const val WINDOW_MIN_HEIGHT_DP = 500

    fun isWideScreen(widthDp: Float): Boolean = widthDp >= WIDE_SCREEN_MIN_WIDTH_DP
}
