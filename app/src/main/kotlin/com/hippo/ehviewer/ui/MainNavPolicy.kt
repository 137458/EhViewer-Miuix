package com.hippo.ehviewer.ui

object MainNavPolicy {
    val ALL_TOP_DESTINATIONS: Set<String> = setOf(
        "HomePageScreenDestination",
        "SubscriptionScreenDestination",
        "WhatshotScreenDestination",
        "ToplistScreenDestination",
        "FavouritesScreenDestination",
        "HistoryScreenDestination",
        "DownloadsScreenDestination",
        "SettingsScreenDestination",
    )

    val PRIMARY_BOTTOM_DESTINATIONS: List<String> = listOf(
        "HomePageScreenDestination",
        "SubscriptionScreenDestination",
        "WhatshotScreenDestination",
        "FavouritesScreenDestination",
        "DownloadsScreenDestination",
        "SettingsScreenDestination",
    )

    fun isTopLevelDestination(destinationName: String?): Boolean {
        return destinationName != null && ALL_TOP_DESTINATIONS.contains(destinationName)
    }

    fun getPrimaryBottomIndex(destinationName: String?): Int {
        return PRIMARY_BOTTOM_DESTINATIONS.indexOf(destinationName)
    }
}
