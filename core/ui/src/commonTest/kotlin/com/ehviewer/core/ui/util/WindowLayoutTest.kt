package com.ehviewer.core.ui.util

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WindowLayoutTest {

    @Test
    fun landscapePhoneKeepsFloatingBottomBar() {
        // 800x412dp 横屏手机：高度放不下 8 项侧栏，必须保留悬浮底栏
        assertEquals(NavigationChrome.BottomBar, WindowLayout(800, 412).navigationChrome)
    }

    @Test
    fun portraitPhoneKeepsFloatingBottomBar() {
        assertEquals(NavigationChrome.BottomBar, WindowLayout(412, 892).navigationChrome)
    }

    @Test
    fun portraitTabletUsesNavigationRail() {
        assertEquals(NavigationChrome.Rail, WindowLayout(800, 1280).navigationChrome)
    }

    @Test
    fun landscapeTabletUsesNavigationRail() {
        assertEquals(NavigationChrome.Rail, WindowLayout(1280, 800).navigationChrome)
    }

    @Test
    fun unfoldedFoldableUsesNavigationRail() {
        assertEquals(NavigationChrome.Rail, WindowLayout(840, 700).navigationChrome)
    }

    @Test
    fun railRequiresBothDimensionsAtBreakpoint() {
        assertEquals(NavigationChrome.Rail, WindowLayout(600, 600).navigationChrome)
        assertEquals(NavigationChrome.BottomBar, WindowLayout(599, 600).navigationChrome)
        assertEquals(NavigationChrome.BottomBar, WindowLayout(600, 599).navigationChrome)
    }

    @Test
    fun landscapeDetectionComparesWidthAndHeight() {
        assertTrue(WindowLayout(800, 412).isLandscape)
        assertFalse(WindowLayout(412, 892).isLandscape)
        assertFalse(WindowLayout(800, 800).isLandscape)
    }

    @Test
    fun shortLandscapeOnlyMatchesLowHeightLandscapeWindows() {
        assertTrue(WindowLayout(800, 412).isShortLandscape)
        assertFalse(WindowLayout(1280, 800).isShortLandscape)
        assertFalse(WindowLayout(412, 892).isShortLandscape)
        assertFalse(WindowLayout(800, 600).isShortLandscape)
    }

    @Test
    fun compactHeightOnlyMatchesShortWindowsRegardlessOfOrientation() {
        assertTrue(WindowLayout(800, 412).isCompactHeight)
        assertTrue(WindowLayout(800, 499).isCompactHeight)
        assertFalse(WindowLayout(800, 500).isCompactHeight)
        assertFalse(WindowLayout(1280, 800).isCompactHeight)
    }

    @Test
    fun wideOnlyMatchesWindowsAtTheWideBreakpoint() {
        assertFalse(WindowLayout(839, 700).isWide)
        assertTrue(WindowLayout(840, 700).isWide)
        assertTrue(WindowLayout(1280, 800).isWide)
    }

    @Test
    fun contentMaxWidthCapsSingleColumnContentFromMediumWidthsUpwards() {
        assertNull(WindowLayout(599, 892).contentMaxWidth)
        assertEquals(640.dp, WindowLayout(600, 892).contentMaxWidth)
        assertEquals(640.dp, WindowLayout(800, 412).contentMaxWidth)
        assertEquals(840.dp, WindowLayout(840, 700).contentMaxWidth)
        assertEquals(840.dp, WindowLayout(1280, 800).contentMaxWidth)
    }

    @Test
    fun isWideContentTracksWhetherContentGetsCapped() {
        assertFalse(WindowLayout(599, 892).isWideContent)
        assertTrue(WindowLayout(600, 892).isWideContent)
        assertTrue(WindowLayout(1280, 800).isWideContent)
    }

    @Test
    fun overlayMaxHeightScalesWithWindowHeightAndIsUnknownWhenSizeIsMissing() {
        assertEquals((412 * 0.6f).dp, WindowLayout(800, 412).overlayMaxHeight)
        assertNull(WindowLayout(0, 0).overlayMaxHeight)
    }

    @Test
    fun thumbGridColumnsGrowsWithWindowWidthButNeverShrinksConfiguredValue() {
        assertEquals(4, WindowLayout.thumbGridColumns(800, configuredColumns = 3))
        assertEquals(3, WindowLayout.thumbGridColumns(412, configuredColumns = 3))
        assertEquals(5, WindowLayout.thumbGridColumns(412, configuredColumns = 5))
        assertEquals(6, WindowLayout.thumbGridColumns(1280, configuredColumns = 3))
        assertEquals(1, WindowLayout.thumbGridColumns(120, configuredColumns = 1))
    }

    @Test
    fun detailMinColumnWidthLeavesRoomForTwoColumnsIncludingSpacing() {
        val availableWidthDp = 776
        val spacingDp = 8
        val minColumnWidth = WindowLayout.detailMinColumnWidth(
            availableWidthDp = availableWidthDp,
            configuredMinWidthDp = 480,
            spacingDp = spacingDp,
        )
        // 保证「至少两列」：两列加中间间距必须放得下
        assertTrue(
            2 * minColumnWidth + spacingDp <= availableWidthDp,
            "minColumnWidth=$minColumnWidth 放不下两列",
        )
    }

    @Test
    fun detailMinColumnWidthKeepsConfiguredWidthOnNarrowWindows() {
        // 竖屏手机不介入用户配置
        assertEquals(480, WindowLayout.detailMinColumnWidth(412, 480, spacingDp = 8))
    }

    @Test
    fun detailMinColumnWidthNeverWidensConfiguredMinimum() {
        // 用户配置本来就比两列还小则不放大
        assertEquals(280, WindowLayout.detailMinColumnWidth(776, 280, spacingDp = 8))
        // 超宽屏仍按配置列宽排更多列
        assertEquals(480, WindowLayout.detailMinColumnWidth(1280, 480, spacingDp = 8))
    }
}
