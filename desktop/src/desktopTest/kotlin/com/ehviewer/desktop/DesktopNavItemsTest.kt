package com.ehviewer.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopNavItemsTest {

    @Test
    fun defaultConfigIsAllItemsVisibleInDeclarationOrder() {
        val config = DesktopNavItems.defaultConfig()
        assertEquals(DesktopNavItem.entries.toList(), config.order)
        assertTrue(config.hidden.isEmpty())
        assertEquals(DesktopNavItem.entries.toList(), DesktopNavItems.visible(config))
    }

    @Test
    fun encodeDecodeRoundTripsOrderAndVisibility() {
        val config = DesktopNavConfig(
            order = listOf(
                DesktopNavItem.Settings,
                DesktopNavItem.Home,
                DesktopNavItem.History,
                DesktopNavItem.Favorites,
                DesktopNavItem.Downloads,
                DesktopNavItem.Subscription,
                DesktopNavItem.Whatshot,
                DesktopNavItem.Toplist,
            ),
            hidden = setOf(DesktopNavItem.Whatshot, DesktopNavItem.Toplist),
        )
        assertEquals(config, DesktopNavItems.decode(DesktopNavItems.encode(config)))
    }

    @Test
    fun decodeBlankOrNullFallsBackToDefault() {
        assertEquals(DesktopNavItems.defaultConfig(), DesktopNavItems.decode(null))
        assertEquals(DesktopNavItems.defaultConfig(), DesktopNavItems.decode(""))
        assertEquals(DesktopNavItems.defaultConfig(), DesktopNavItems.decode("   "))
    }

    @Test
    fun decodeToleratesUnknownTokensAndAppendsMissingItems() {
        // 未知/空 token 忽略；缺失项按声明顺序补到末尾（新增导航项时旧配置仍可用）
        val config = DesktopNavItems.decode("History, !Favorites ,Bogus,,Home")
        assertEquals(
            listOf(
                DesktopNavItem.History,
                DesktopNavItem.Favorites,
                DesktopNavItem.Home,
                DesktopNavItem.Subscription,
                DesktopNavItem.Whatshot,
                DesktopNavItem.Toplist,
                DesktopNavItem.Downloads,
                DesktopNavItem.Settings,
            ),
            config.order,
        )
        assertEquals(setOf(DesktopNavItem.Favorites), config.hidden)
    }

    @Test
    fun decodeIgnoresDuplicateTokensKeepingFirstOccurrence() {
        val config = DesktopNavItems.decode("Home,History,Home")
        val expected = listOf(DesktopNavItem.Home, DesktopNavItem.History) +
            DesktopNavItem.entries.toList().filter { it != DesktopNavItem.Home && it != DesktopNavItem.History }
        assertEquals(expected, config.order)
        assertTrue(config.hidden.isEmpty())
    }

    @Test
    fun toggleHiddenHidesAndShowsItem() {
        val base = DesktopNavItems.defaultConfig()
        val hidden = DesktopNavItems.toggleHidden(base, DesktopNavItem.Toplist)
        assertTrue(DesktopNavItem.Toplist in hidden.hidden)
        assertEquals(DesktopNavItem.entries.toList().minus(DesktopNavItem.Toplist), DesktopNavItems.visible(hidden))

        assertEquals(base, DesktopNavItems.toggleHidden(hidden, DesktopNavItem.Toplist))
    }

    @Test
    fun toggleHiddenRefusesToHideLastVisibleItem() {
        // 边界：至少保留一个可见导航项，避免导航外壳整体消失
        val onlyHome = DesktopNavConfig(
            order = DesktopNavItem.entries.toList(),
            hidden = DesktopNavItem.entries.toList().minus(DesktopNavItem.Home).toSet(),
        )
        assertEquals(onlyHome, DesktopNavItems.toggleHidden(onlyHome, DesktopNavItem.Home))
    }

    @Test
    fun moveReordersItemAndClampsTargetIndex() {
        val base = DesktopNavItems.defaultConfig()
        val moved = DesktopNavItems.move(base, DesktopNavItem.Downloads, 0)
        assertEquals(DesktopNavItem.Downloads, moved.order.first())
        assertEquals(
            listOf(DesktopNavItem.Downloads) + DesktopNavItem.entries.toList().minus(DesktopNavItem.Downloads),
            moved.order,
        )
        // 边界：越界目标索引钳制到首/末位；移动不改变显隐集合
        assertEquals(DesktopNavItem.Settings, DesktopNavItems.move(base, DesktopNavItem.Settings, -5).order.first())
        assertEquals(DesktopNavItem.Home, DesktopNavItems.move(base, DesktopNavItem.Home, 99).order.last())
        assertEquals(base.hidden, DesktopNavItems.move(base, DesktopNavItem.Home, 3).hidden)
    }
}
