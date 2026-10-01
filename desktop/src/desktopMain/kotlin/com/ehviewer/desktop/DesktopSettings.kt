package com.ehviewer.desktop

import com.ehviewer.core.preferences.DataStorePreferences

// 桌面侧独立偏好存储，复用共享 DataStore 机制与委托链路，与移动端互不影响
object DesktopSettings : DataStorePreferences("desktop") {
    var windowWidth by intPref("window_width", 1280)
    var windowHeight by intPref("window_height", 800)

    // 0 = 跟随系统，1 = 浅色，2 = 深色；以 delegate 暴露供响应式消费（与 app Settings 惯例一致）
    val themeMode = intPref("theme_mode", 0)

    // HTTP 代理，格式 host:port，空 = 直连（无系统代理自动探测）
    val proxy = stringOrNullPref("proxy")

    // true = 关闭窗口时最小化至系统托盘防误触，false = 直接退出应用
    val closeToTray = boolPref("close_to_tray", false)
}
