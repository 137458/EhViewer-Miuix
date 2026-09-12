package com.hippo.ehviewer.ui

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

    fun isTopLevelDestination(destination: Any?): Boolean {
        return destination.routeKey() in ALL_TOP_DESTINATIONS
    }

    fun getPrimaryBottomIndex(
        destination: Any?,
        primaryDestinations: List<Direction> = PRIMARY_BOTTOM_DESTINATIONS,
    ): Int {
        val primaryRoute = when (destination.routeKey()) {
            ToplistScreenDestination.route -> WhatshotScreenDestination.route
            HistoryScreenDestination.route -> DownloadsScreenDestination.route
            else -> destination.routeKey()
        }
        return primaryDestinations.indexOfFirst { it.route == primaryRoute }
    }

    private fun Any?.routeKey(): String? = when (this) {
        is Route -> route
        else -> null
    }
}
