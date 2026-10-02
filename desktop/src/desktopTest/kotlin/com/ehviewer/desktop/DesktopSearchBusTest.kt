package com.ehviewer.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopSearchBusTest {
    @Test
    fun requestAcceptsNonBlankQueries() {
        DesktopSearchBus.consume()
        DesktopSearchBus.request("artist:foo")
        assertEquals("artist:foo", DesktopSearchBus.pendingQuery)
    }

    @Test
    fun requestTrimsAndRejectsBlank() {
        DesktopSearchBus.consume()
        DesktopSearchBus.request("   ")
        assertNull(DesktopSearchBus.pendingQuery)
        DesktopSearchBus.request("  bar  ")
        assertEquals("bar", DesktopSearchBus.pendingQuery)
    }

    @Test
    fun consumeClearsPending() {
        DesktopSearchBus.consume()
        DesktopSearchBus.request("baz")
        DesktopSearchBus.consume()
        assertNull(DesktopSearchBus.pendingQuery)
    }
}
