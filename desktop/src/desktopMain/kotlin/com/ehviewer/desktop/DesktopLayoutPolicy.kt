package com.ehviewer.desktop

// 桌面窗口级约束。宽窄断点、导航形态与内容限宽一律走共享体系：
// core/ui AdaptiveBreakpoints + WindowLayout（LocalWindowLayout 由壳层 Main.kt 提供），
// 断点常量不再桌面自持，避免与移动端双轨漂移。
object DesktopLayoutPolicy {
    const val WINDOW_MIN_WIDTH_DP = 600
    const val WINDOW_MIN_HEIGHT_DP = 500
}
