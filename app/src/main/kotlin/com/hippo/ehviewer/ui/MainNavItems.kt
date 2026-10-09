package com.hippo.ehviewer.ui

// 底栏导航项集合：显隐与顺序可配置，编解码约定与桌面 DesktopNavItems 一致
// （单字符串偏好，隐藏项以 '!' 前缀标记）。声明顺序即默认顺序。
enum class MainNavItem {
    Home,
    Subscription,
    Whatshot,
    Toplist,
    Favorites,
    History,
    Downloads,
    Settings,
}

data class MainNavConfig(
    val order: List<MainNavItem>,
    val hidden: Set<MainNavItem>,
)

object MainNavItems {
    private const val HIDDEN_PREFIX = "!"

    // 手机底栏默认沿用历史行为：排行榜/历史不在底栏（经设置页「未显示页面」入口可达）
    fun defaultConfig(): MainNavConfig = MainNavConfig(
        order = MainNavItem.entries.toList(),
        hidden = setOf(MainNavItem.Toplist, MainNavItem.History),
    )

    fun visible(config: MainNavConfig): List<MainNavItem> = config.order.filterNot { it in config.hidden }

    // 显隐切换；拒绝隐藏最后一个可见项（底栏导航整体消失会锁死入口）
    fun toggleHidden(config: MainNavConfig, item: MainNavItem): MainNavConfig = when {
        item in config.hidden -> config.copy(hidden = config.hidden - item)
        visible(config).size <= 1 -> config
        else -> config.copy(hidden = config.hidden + item)
    }

    // 拖拽排序：把 item 移动到 toIndex（越界钳制到首/末位），显隐集合保持不变
    fun move(config: MainNavConfig, item: MainNavItem, toIndex: Int): MainNavConfig {
        val from = config.order.indexOf(item)
        if (from < 0) return config
        val to = toIndex.coerceIn(0, config.order.lastIndex)
        if (from == to) return config
        val mutable = config.order.toMutableList()
        mutable.removeAt(from)
        mutable.add(to, item)
        return config.copy(order = mutable)
    }

    fun encode(config: MainNavConfig): String = config.order.joinToString(",") { item ->
        if (item in config.hidden) "$HIDDEN_PREFIX${item.name}" else item.name
    }

    // 解码对未知/重复/缺失 token 容错，保证新增导航项时旧配置仍可用
    fun decode(raw: String?): MainNavConfig {
        if (raw.isNullOrBlank()) return defaultConfig()
        val seen = LinkedHashSet<MainNavItem>()
        val hidden = mutableSetOf<MainNavItem>()
        raw.split(',').forEach { token ->
            val trimmed = token.trim()
            if (trimmed.isEmpty()) return@forEach
            val isHidden = trimmed.startsWith(HIDDEN_PREFIX)
            val name = trimmed.removePrefix(HIDDEN_PREFIX)
            val item = MainNavItem.entries.firstOrNull { it.name == name } ?: return@forEach
            // 重复 token 保留首次出现的显隐语义
            if (seen.add(item) && isHidden) hidden += item
        }
        MainNavItem.entries.forEach { if (it !in seen) seen += it }
        return MainNavConfig(seen.toList(), hidden)
    }
}
