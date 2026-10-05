package com.ehviewer.desktop

import com.ehviewer.core.preferences.DataStorePreferences

// 桌面侧独立偏好存储，复用共享 DataStore 机制与委托链路，与移动端互不影响
object DesktopSettings : DataStorePreferences("desktop") {
    var windowWidth by intPref("window_width", 1280)
    var windowHeight by intPref("window_height", 800)

    // 主窗口位置（dp）；Int.MIN_VALUE 表示未记忆（跟随平台默认位置）——负数是左侧/上方显示器的合法坐标，不可作哨兵
    var windowX by intPref("window_x", Int.MIN_VALUE)
    var windowY by intPref("window_y", Int.MIN_VALUE)

    // 0 = 跟随系统，1 = 浅色，2 = 深色；以 delegate 暴露供响应式消费（与 app Settings 惯例一致）
    val themeMode = intPref("theme_mode", 0)

    // 深色模式下使用 AMOLED 纯黑配色（与移动端 blackDarkTheme 同语义）
    val blackDarkTheme = boolPref("black_dark_theme", false)

    // HTTP 代理，格式 host:port，空 = 直连（无系统代理自动探测）
    val proxy = stringOrNullPref("proxy")

    // true = 关闭窗口时最小化至系统托盘防误触，false = 直接退出应用
    val closeToTray = boolPref("close_to_tray", false)

    // 会话恢复窗口上限（1-20，非法值经 sanitizeRestoreLimit 合法化）
    val restoreLimit = intPref("restore_limit", 10)

    // 0 = 列表视图 (List)，1 = 网格卡片视图 (Grid)
    val viewMode = intPref("view_mode", 0)

    // 图片保存目录；null/空 = 默认下载目录 ~/Downloads/EhViewer
    val imageSaveDir = stringOrNullPref("image_save_dir")

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

    // 阅读进度（"gid:page,..." 编解码经 DesktopReadingProgress，跨会话续读）
    val readingProgress = stringPref("reader_progress", "")

    // 导航栏样式：0 = 自动（宽屏侧栏/窄屏底栏），1 = 侧边导航栏 (Rail)，2 = 底部液态玻璃悬浮栏 (FloatingBottomBar)
    val navBarStyle = intPref("nav_bar_style", 0)

    // false = 启动不恢复上次会话窗口，true = 恢复
    val restoreSession = boolPref("restore_session", true)
}

enum class DesktopNavBarStyle(val value: Int) {
    Auto(0),
    Rail(1),
    FloatingBottomBar(2),
    ;

    companion object {
        fun fromValue(value: Int): DesktopNavBarStyle = entries.firstOrNull { it.value == value } ?: Auto
    }
}
