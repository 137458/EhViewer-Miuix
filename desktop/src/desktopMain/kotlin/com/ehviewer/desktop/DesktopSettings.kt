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

    // 0 = 列表视图 (List)，1 = 网格卡片视图 (Grid)
    val viewMode = intPref("view_mode", 0)

    // 搜索历史记录（多行编码字符串，持久化跨会话）
    val searchHistory = stringPref("search_history", "")

    // 上次会话打开的画廊窗口快照（JSON 数组，启动时恢复；窗口列表清空视为退出不覆盖）
    val sessionGalleries = stringPref("session_galleries", "")

    // 画廊列表排序（FIELD:DIRECTION，跨会话保留）
    val sortConfig = stringPref("sort_config", "")

    // 上次浏览的主库 Tab（History/Favorites/Online，跨会话保留）
    val lastTab = stringPref("last_tab", "History")

    // 阅读方向（LTR 西式 / RTL 日漫），影响阅读器点击区域与方向键语义
    val readingDirection = stringPref("reading_direction", "LTR")

    // false = 启动不恢复上次会话窗口，true = 恢复
    val restoreSession = boolPref("restore_session", true)
}
