package com.hippo.ehviewer.ui

import com.hippo.ehviewer.ui.destinations.DownloadsScreenDestination
import com.hippo.ehviewer.ui.destinations.FavouritesScreenDestination
import com.hippo.ehviewer.ui.destinations.GalleryCommentsScreenDestination
import com.hippo.ehviewer.ui.destinations.HistoryScreenDestination
import com.hippo.ehviewer.ui.destinations.HomePageScreenDestination
import com.hippo.ehviewer.ui.destinations.SettingsScreenDestination
import com.hippo.ehviewer.ui.destinations.SubscriptionScreenDestination
import com.hippo.ehviewer.ui.destinations.ToplistScreenDestination
import com.hippo.ehviewer.ui.destinations.WhatshotScreenDestination
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigationRoutingTest {

    @Test
    fun `all main destinations remain available as top-level navigation`() {
        val expectedDestinations = setOf(
            HomePageScreenDestination,
            SubscriptionScreenDestination,
            WhatshotScreenDestination,
            ToplistScreenDestination,
            FavouritesScreenDestination,
            HistoryScreenDestination,
            DownloadsScreenDestination,
            SettingsScreenDestination,
        )

        assertTrue(expectedDestinations.all(MainNavPolicy::isTopLevelDestination))
        assertFalse(MainNavPolicy.isTopLevelDestination(GalleryCommentsScreenDestination(1L)))
    }

    @Test
    fun `secondary destinations select their related primary tab in the active layout`() {
        val activeBottomDestinations = listOf(
            HomePageScreenDestination,
            SubscriptionScreenDestination,
            WhatshotScreenDestination,
            FavouritesScreenDestination,
            DownloadsScreenDestination,
            SettingsScreenDestination,
        )

        assertEquals(2, MainNavPolicy.getPrimaryBottomIndex(ToplistScreenDestination, activeBottomDestinations))
        assertEquals(4, MainNavPolicy.getPrimaryBottomIndex(HistoryScreenDestination, activeBottomDestinations))
    }

    @Test
    fun `secondary mapping follows the supplied layout order`() {
        val activeBottomDestinations = listOf(
            SettingsScreenDestination,
            DownloadsScreenDestination,
            WhatshotScreenDestination,
            HomePageScreenDestination,
        )

        assertEquals(2, MainNavPolicy.getPrimaryBottomIndex(ToplistScreenDestination, activeBottomDestinations))
        assertEquals(1, MainNavPolicy.getPrimaryBottomIndex(HistoryScreenDestination, activeBottomDestinations))
    }

    @Test
    fun `unknown and missing destinations are rejected without a bottom bar selection`() {
        assertFalse(MainNavPolicy.isTopLevelDestination(null))
        assertFalse(MainNavPolicy.isTopLevelDestination(GalleryCommentsScreenDestination(1L)))
        assertEquals(-1, MainNavPolicy.getPrimaryBottomIndex(null))
        assertEquals(-1, MainNavPolicy.getPrimaryBottomIndex(GalleryCommentsScreenDestination(1L)))
    }
}
