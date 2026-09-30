package com.ehviewer.desktop

import com.ehviewer.core.preferences.DataStorePreferences

// 桌面侧独立偏好存储，复用共享 DataStore 机制与委托链路，与移动端互不影响
object DesktopSettings : DataStorePreferences("desktop") {
    var windowWidth by intPref("window_width", 1280)
    var windowHeight by intPref("window_height", 800)
}
