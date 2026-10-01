package com.ehviewer.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopViewModeTest {
    @Test
    fun fromOrdinal_mapsKnownOrdinalsAndDefaultsToList() {
        assertEquals(DesktopViewMode.List, DesktopViewMode.fromOrdinal(0))
        assertEquals(DesktopViewMode.Grid, DesktopViewMode.fromOrdinal(1))
        // 三角验证：负数、超大整数、极端边界
        assertEquals(DesktopViewMode.List, DesktopViewMode.fromOrdinal(-1))
        assertEquals(DesktopViewMode.List, DesktopViewMode.fromOrdinal(99))
        assertEquals(DesktopViewMode.List, DesktopViewMode.fromOrdinal(Int.MIN_VALUE))
        assertEquals(DesktopViewMode.List, DesktopViewMode.fromOrdinal(Int.MAX_VALUE))
    }

    @Test
    fun toggle_alternatesBetweenListAndGrid() {
        assertEquals(DesktopViewMode.Grid, DesktopViewMode.List.toggle())
        assertEquals(DesktopViewMode.List, DesktopViewMode.Grid.toggle())
        // 三角验证：多次往返翻转
        assertEquals(DesktopViewMode.List, DesktopViewMode.List.toggle().toggle())
        assertEquals(DesktopViewMode.Grid, DesktopViewMode.Grid.toggle().toggle())
    }
}
