package com.ehviewer.desktop

import com.ehviewer.core.i18n.MR
import dev.icerock.moko.resources.StringResource

// 桌面导航项集合：可配置顺序与显隐（设计文档 §4.5 / §6.6）。声明顺序即默认顺序。
enum class DesktopNavItem {
    Home,
    Subscription,
    Whatshot,
    Toplist,
    Favorites,
    History,
    Downloads,
    Settings,
    ;

    // 库页 Tab 项与设置项的区分：库项驱动主库 Tab，设置项为独立页面
    fun toLibraryTab(): LibraryTab? = when (this) {
        Home -> LibraryTab.Home
        Subscription -> LibraryTab.Subscription
        Whatshot -> LibraryTab.Whatshot
        Toplist -> LibraryTab.Toplist
        Favorites -> LibraryTab.Favorites
        History -> LibraryTab.History
        Downloads -> LibraryTab.Downloads
        Settings -> null
    }

    val titleRes: StringResource
        get() = toLibraryTab()?.titleRes ?: MR.strings.menu_settings
}

// 导航配置：order 为完整顺序（含隐藏项，保证恢复显隐时位置不丢），hidden 为不渲染的项
data class DesktopNavConfig(
    val order: List<DesktopNavItem>,
    val hidden: Set<DesktopNavItem>,
)

// 导航项配置纯逻辑：编解码走单字符串偏好（与项目既有 search_history/sort_config 惯例一致），
// 隐藏项以 '!' 前缀标记；解码对未知/重复/缺失 token 容错，保证新增导航项时旧配置仍可用。
object DesktopNavItems {
    private const val HIDDEN_PREFIX = "!"

    fun defaultConfig(): DesktopNavConfig = DesktopNavConfig(DesktopNavItem.entries.toList(), emptySet())

    fun encode(config: DesktopNavConfig): String = config.order.joinToString(",") { item ->
        if (item in config.hidden) "$HIDDEN_PREFIX${item.name}" else item.name
    }

    fun decode(raw: String?): DesktopNavConfig {
        if (raw.isNullOrBlank()) return defaultConfig()
        val seen = LinkedHashSet<DesktopNavItem>()
        val hidden = mutableSetOf<DesktopNavItem>()
        raw.split(',').forEach { token ->
            val trimmed = token.trim()
            if (trimmed.isEmpty()) return@forEach
            val isHidden = trimmed.startsWith(HIDDEN_PREFIX)
            val name = trimmed.removePrefix(HIDDEN_PREFIX)
            val item = DesktopNavItem.entries.firstOrNull { it.name == name } ?: return@forEach
            // 重复 token 保留首次出现的显隐语义
            if (seen.add(item) && isHidden) hidden += item
        }
        DesktopNavItem.entries.forEach { if (it !in seen) seen += it }
        return DesktopNavConfig(seen.toList(), hidden)
    }

    fun visible(config: DesktopNavConfig): List<DesktopNavItem> = config.order.filterNot { it in config.hidden }

    // 显隐切换；拒绝隐藏最后一个可见项（导航外壳整体消失会锁死入口）
    fun toggleHidden(config: DesktopNavConfig, item: DesktopNavItem): DesktopNavConfig = when {
        item in config.hidden -> config.copy(hidden = config.hidden - item)
        visible(config).size <= 1 -> config
        else -> config.copy(hidden = config.hidden + item)
    }

    // 拖拽排序：把 item 移动到 toIndex（越界钳制到首/末位），显隐集合保持不变
    fun move(config: DesktopNavConfig, item: DesktopNavItem, toIndex: Int): DesktopNavConfig {
        val from = config.order.indexOf(item)
        if (from < 0) return config
        val to = toIndex.coerceIn(0, config.order.lastIndex)
        if (from == to) return config
        val mutable = config.order.toMutableList()
        mutable.removeAt(from)
        mutable.add(to, item)
        return config.copy(order = mutable)
    }
}
