package com.hippo.ehviewer.ui

import com.ehviewer.core.i18n.R
import com.hippo.ehviewer.ui.destinations.DownloadsScreenDestination
import com.hippo.ehviewer.ui.destinations.FavouritesScreenDestination
import com.hippo.ehviewer.ui.destinations.HistoryScreenDestination
import com.hippo.ehviewer.ui.destinations.HomePageScreenDestination
import com.hippo.ehviewer.ui.destinations.SettingsScreenDestination
import com.hippo.ehviewer.ui.destinations.SubscriptionScreenDestination
import com.hippo.ehviewer.ui.destinations.ToplistScreenDestination
import com.hippo.ehviewer.ui.destinations.WhatshotScreenDestination
import com.ramcosta.composedestinations.spec.Direction
import com.ramcosta.composedestinations.spec.Route

object MainNavPolicy {
    val ALL_TOP_DESTINATIONS: Set<String> = setOf(
        HomePageScreenDestination.route,
        SubscriptionScreenDestination.route,
        WhatshotScreenDestination.route,
        ToplistScreenDestination.route,
        FavouritesScreenDestination.route,
        HistoryScreenDestination.route,
        DownloadsScreenDestination.route,
        SettingsScreenDestination.route,
    )

    val PRIMARY_BOTTOM_DESTINATIONS: List<Direction> = listOf(
        HomePageScreenDestination,
        SubscriptionScreenDestination,
        WhatshotScreenDestination,
        FavouritesScreenDestination,
        DownloadsScreenDestination,
        SettingsScreenDestination,
    )

    fun isTopLevelDestination(destination: Any?): Boolean = destination.routeKey() in ALL_TOP_DESTINATIONS

    fun getPrimaryBottomIndex(
        destination: Any?,
        primaryDestinations: List<Direction> = PRIMARY_BOTTOM_DESTINATIONS,
    ): Int {
        val route = destination.routeKey()
        // 底栏可配置后 Toplist/History 可能本身就在栏内，先精确匹配；仅当不在栏内时
        // 才退回二级页到相邻主 Tab 的别名映射，别名也不在栏内则无选中项
        primaryDestinations.indexOfFirst { it.route == route }.takeIf { it >= 0 }?.let { return it }
        val primaryRoute = when (route) {
            ToplistScreenDestination.route -> WhatshotScreenDestination.route
            HistoryScreenDestination.route -> DownloadsScreenDestination.route
            else -> route
        }
        return primaryDestinations.indexOfFirst { it.route == primaryRoute }
    }

    private fun Any?.routeKey(): String? = when (this) {
        is Route -> route
        else -> null
    }
}

val MainNavItem.direction: Direction
    get() = when (this) {
        MainNavItem.Home -> HomePageScreenDestination
        MainNavItem.Subscription -> SubscriptionScreenDestination
        MainNavItem.Whatshot -> WhatshotScreenDestination
        MainNavItem.Toplist -> ToplistScreenDestination
        MainNavItem.Favorites -> FavouritesScreenDestination
        MainNavItem.History -> HistoryScreenDestination
        MainNavItem.Downloads -> DownloadsScreenDestination
        MainNavItem.Settings -> SettingsScreenDestination
    }

val MainNavItem.titleRes: Int
    get() = when (this) {
        MainNavItem.Home -> R.string.homepage
        MainNavItem.Subscription -> R.string.subscription
        MainNavItem.Whatshot -> R.string.whats_hot
        MainNavItem.Toplist -> R.string.toplist
        MainNavItem.Favorites -> R.string.favourite
        MainNavItem.History -> R.string.history
        MainNavItem.Downloads -> R.string.downloads
        MainNavItem.Settings -> R.string.settings
    }
