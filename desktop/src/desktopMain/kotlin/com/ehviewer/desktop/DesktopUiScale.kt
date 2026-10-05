package com.ehviewer.desktop

// 应用内界面缩放（设计文档 §4.7）：DPI 感知失效时的兜底。
// 经 LocalDensity 覆盖整体放大/缩小，文字与矢量图按新密度重绘保持清晰（非位图放大）。
object DesktopUiScale {
    val OPTIONS = listOf(80, 90, 100, 110, 125, 150)
    const val DEFAULT = 100

    // 未列入档位的值一律回默认，避免界面被放大到不可用
    fun sanitize(raw: Int): Int = if (raw in OPTIONS) raw else DEFAULT

    fun factor(raw: Int): Float = sanitize(raw) / 100f
}

// 启动层 DPI 感知：必须在任何 AWT/Swing 类初始化前调用（main() 首行）。
// fat jar 走系统 JRE 时，若进程被 java.exe manifest 锁为 system-DPI-aware，高缩放显示器上
// 窗口内容会被系统位图放大而发虚；此处显式声明 DPI 感知，请求 JVM 走 HiDPI 渲染路径。
object DesktopDpi {
    const val PROPERTY_DPI_AWARE = "sun.java2d.dpiaware"
    const val PROPERTY_UI_SCALE_ENABLED = "sun.java2d.uiScale.enabled"

    fun install() {
        System.setProperty(PROPERTY_DPI_AWARE, "true")
        System.setProperty(PROPERTY_UI_SCALE_ENABLED, "true")
    }
}
