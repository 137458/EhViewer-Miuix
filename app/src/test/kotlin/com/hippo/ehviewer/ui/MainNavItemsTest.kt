package com.hippo.ehviewer.ui

import com.hippo.ehviewer.ui.destinations.DownloadsScreenDestination
import com.hippo.ehviewer.ui.destinations.HomePageScreenDestination
import com.hippo.ehviewer.ui.destinations.SettingsScreenDestination
import com.hippo.ehviewer.ui.destinations.ToplistScreenDestination
import com.hippo.ehviewer.ui.destinations.WhatshotScreenDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainNavItemsTest {

    @Test
    fun `default config keeps phone bottom bar baseline`() {
        val config = MainNavItems.defaultConfig()
        assertEquals(MainNavItem.entries.toList(), config.order)
        assertEquals(setOf(MainNavItem.Toplist, MainNavItem.History), config.hidden)
        assertEquals(
            listOf(MainNavItem.Home, MainNavItem.Subscription, MainNavItem.Whatshot, MainNavItem.Favorites, MainNavItem.Downloads, MainNavItem.Settings),
            MainNavItems.visible(config),
        )
    }

    @Test
    fun `decode tolerates unknown tokens and appends missing ones`() {
        val config = MainNavItems.decode("Home,Whatever,!Downloads")
        // 已见项保留解析位置，缺失项按声明顺序补在尾部
        assertEquals(
            listOf(
                MainNavItem.Home,
                MainNavItem.Downloads,
                MainNavItem.Subscription,
                MainNavItem.Whatshot,
                MainNavItem.Toplist,
                MainNavItem.Favorites,
                MainNavItem.History,
                MainNavItem.Settings,
            ),
            config.order,
        )
        assertEquals(setOf(MainNavItem.Downloads), config.hidden)
    }

    @Test
    fun `decode keeps first occurrence semantics for duplicates`() {
        val config = MainNavItems.decode("!Toplist,Toplist")
        assertTrue(MainNavItem.Toplist in config.hidden)
        val config2 = MainNavItems.decode("Toplist,!Toplist")
        assertTrue(MainNavItem.Toplist !in config2.hidden)
    }

    @Test
    fun `encode decode roundtrips`() {
        val config = MainNavConfig(
            order = listOf(MainNavItem.Settings, MainNavItem.Home, MainNavItem.Downloads, MainNavItem.Whatshot, MainNavItem.Favorites, MainNavItem.Subscription, MainNavItem.Toplist, MainNavItem.History),
            hidden = setOf(MainNavItem.Toplist, MainNavItem.Subscription),
        )
        assertEquals(config, MainNavItems.decode(MainNavItems.encode(config)))
    }

    @Test
    fun `toggle hidden guards the last visible item`() {
        // 设置页固定可见后，底栏只剩一项只可能发生在其被移出 order 的情形，守卫仍需生效
        val config = MainNavConfig(order = listOf(MainNavItem.Home, MainNavItem.Downloads), hidden = setOf(MainNavItem.Downloads))
        val guarded = MainNavItems.toggleHidden(config, MainNavItem.Home)
        assertEquals(config, guarded)
        val unhidden = MainNavItems.toggleHidden(config, MainNavItem.Downloads)
        assertTrue(MainNavItem.Downloads !in unhidden.hidden)
    }

    @Test
    fun `settings can never be hidden`() {
        val config = MainNavItems.defaultConfig()
        assertEquals(config, MainNavItems.toggleHidden(config, MainNavItem.Settings))
        assertFalse(MainNavItems.canHide(MainNavItem.Settings))
        assertTrue(MainNavItems.canHide(MainNavItem.Home))
    }

    @Test
    fun `visible keeps settings even when config marks it hidden`() {
        val broken = MainNavConfig(MainNavItem.entries.toList(), MainNavItem.entries.toSet())
        assertEquals(listOf(MainNavItem.Settings), MainNavItems.visible(broken))
    }

    @Test
    fun `decode and encode ignore hidden marker on settings`() {
        val decoded = MainNavItems.decode("!Settings,Home,!Downloads")
        assertTrue(MainNavItem.Settings !in decoded.hidden)
        assertTrue(MainNavItem.Downloads in decoded.hidden)
        val encoded = MainNavItems.encode(MainNavConfig(MainNavItem.entries.toList(), setOf(MainNavItem.Settings)))
        assertFalse("!Settings" in encoded)
        // 即便强制写入隐藏集合，序列化结果也不得带 !Settings 标记
        assertEquals("Home,Subscription,Whatshot,Toplist,Favorites,History,Downloads,Settings", encoded)
    }

    @Test
    fun `move reorders and clamps out of range target`() {
        val config = MainNavItems.defaultConfig()
        val moved = MainNavItems.move(config, MainNavItem.Home, 100)
        assertEquals(MainNavItem.Home, moved.order.last())
        assertEquals(config.order - MainNavItem.Home, moved.order.dropLast(1))
        assertEquals(config.hidden, moved.hidden)
        assertEquals(config, MainNavItems.move(config, MainNavItem.Home, 0))
    }

    @Test
    fun `empty or blank pref decodes to default`() {
        assertEquals(MainNavItems.defaultConfig(), MainNavItems.decode(null))
        assertEquals(MainNavItems.defaultConfig(), MainNavItems.decode(""))
    }

    @Test
    fun `exact route match wins over secondary alias`() {
        val bar = listOf(HomePageScreenDestination, ToplistScreenDestination, SettingsScreenDestination)
        assertEquals(1, MainNavPolicy.getPrimaryBottomIndex(ToplistScreenDestination, bar))
    }

    @Test
    fun `secondary falls back to alias and to no selection when alias absent`() {
        val bar = listOf(HomePageScreenDestination, WhatshotScreenDestination, DownloadsScreenDestination, SettingsScreenDestination)
        assertEquals(1, MainNavPolicy.getPrimaryBottomIndex(ToplistScreenDestination, bar))
        val noAlias = listOf(HomePageScreenDestination, SettingsScreenDestination)
        assertEquals(-1, MainNavPolicy.getPrimaryBottomIndex(ToplistScreenDestination, noAlias))
    }

    @Test
    fun `default bottom destinations still resolve indices`() {
        val bar = MainNavItems.visible(MainNavItems.defaultConfig()).map { it.direction }
        assertEquals(HomePageScreenDestination, bar.first())
        assertEquals(SettingsScreenDestination, bar.last())
        assertEquals(0, MainNavPolicy.getPrimaryBottomIndex(HomePageScreenDestination, bar))
        assertEquals(2, MainNavPolicy.getPrimaryBottomIndex(WhatshotScreenDestination, bar))
        assertEquals(2, MainNavPolicy.getPrimaryBottomIndex(ToplistScreenDestination, bar))
    }
}
