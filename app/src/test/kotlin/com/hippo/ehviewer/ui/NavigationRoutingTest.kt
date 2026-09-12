package com.hippo.ehviewer.ui

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigationRoutingTest {

    @Test
    fun `navItems has 8 destinations including Toplist and History`() {
        assertEquals(8, MainNavPolicy.ALL_TOP_DESTINATIONS.size)
        assertTrue(MainNavPolicy.ALL_TOP_DESTINATIONS.contains("ToplistScreenDestination"))
        assertTrue(MainNavPolicy.ALL_TOP_DESTINATIONS.contains("HistoryScreenDestination"))
    }

    @Test
    fun `primary bottom bar has 6 destinations for mobile layout`() {
        assertEquals(6, MainNavPolicy.PRIMARY_BOTTOM_DESTINATIONS.size)
    }

    @Test
    fun `when destination is Toplist or History, it is recognized as a valid top destination`() {
        // Ensuring user is not stranded without top-level navigation
        assertTrue(MainNavPolicy.isTopLevelDestination("ToplistScreenDestination"))
        assertTrue(MainNavPolicy.isTopLevelDestination("HistoryScreenDestination"))
        assertFalse(MainNavPolicy.isTopLevelDestination("GalleryCommentsScreenDestination"))
    }

    @Test
    fun `when destination is not in primary bottom items, bottom bar index evaluates safely to negative 1 without crashing`() {
        val index = MainNavPolicy.getPrimaryBottomIndex("ToplistScreenDestination")
        assertEquals(-1, index)
    }
}
