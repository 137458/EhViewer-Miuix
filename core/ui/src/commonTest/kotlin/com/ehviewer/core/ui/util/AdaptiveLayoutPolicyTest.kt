package com.ehviewer.core.ui.util

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AdaptiveLayoutPolicyTest {

    @Test
    fun landscapePhoneKeepsFloatingBottomBar() {
        // 800x412dp 横屏手机：高度放不下 8 项侧栏，必须保留悬浮底栏
        assertEquals(NavigationChrome.BottomBar, AdaptiveLayoutPolicy.navigationChrome(800, 412))
    }

    @Test
    fun portraitPhoneKeepsFloatingBottomBar() {
        assertEquals(NavigationChrome.BottomBar, AdaptiveLayoutPolicy.navigationChrome(412, 892))
    }

    @Test
    fun portraitTabletUsesNavigationRail() {
        assertEquals(NavigationChrome.Rail, AdaptiveLayoutPolicy.navigationChrome(800, 1280))
    }

    @Test
    fun landscapeTabletUsesNavigationRail() {
        assertEquals(NavigationChrome.Rail, AdaptiveLayoutPolicy.navigationChrome(1280, 800))
    }

    @Test
    fun unfoldedFoldableUsesNavigationRail() {
        assertEquals(NavigationChrome.Rail, AdaptiveLayoutPolicy.navigationChrome(840, 700))
    }

    @Test
    fun railRequiresBothDimensionsAtBreakpoint() {
        assertEquals(NavigationChrome.Rail, AdaptiveLayoutPolicy.navigationChrome(600, 600))
        assertEquals(NavigationChrome.BottomBar, AdaptiveLayoutPolicy.navigationChrome(599, 600))
        assertEquals(NavigationChrome.BottomBar, AdaptiveLayoutPolicy.navigationChrome(600, 599))
    }

    @Test
    fun landscapeDetectionComparesWidthAndHeight() {
        assertEquals(true, AdaptiveLayoutPolicy.isLandscape(800, 412))
        assertEquals(false, AdaptiveLayoutPolicy.isLandscape(412, 892))
        assertEquals(false, AdaptiveLayoutPolicy.isLandscape(800, 800))
    }

    @Test
    fun shortLandscapeOnlyMatchesLowHeightLandscapeWindows() {
        assertEquals(true, AdaptiveLayoutPolicy.isShortLandscape(800, 412))
        assertEquals(false, AdaptiveLayoutPolicy.isShortLandscape(1280, 800))
        assertEquals(false, AdaptiveLayoutPolicy.isShortLandscape(412, 892))
        assertEquals(false, AdaptiveLayoutPolicy.isShortLandscape(800, 600))
    }

    @Test
    fun contentMaxWidthCapsSingleColumnContentFromMediumWidthsUpwards() {
        assertNull(AdaptiveLayoutPolicy.contentMaxWidth(599))
        assertEquals(640.dp, AdaptiveLayoutPolicy.contentMaxWidth(600))
        assertEquals(640.dp, AdaptiveLayoutPolicy.contentMaxWidth(800))
        assertEquals(840.dp, AdaptiveLayoutPolicy.contentMaxWidth(840))
        assertEquals(840.dp, AdaptiveLayoutPolicy.contentMaxWidth(1280))
    }

    @Test
    fun detailMinColumnWidthForcesTwoColumnsOnWideWindows() {
        // 776dp 可用宽 + 480dp 配置列宽 → 压到 388dp，Adaptive 可排两列
        assertEquals(388, AdaptiveLayoutPolicy.detailMinColumnWidth(776, 480))
        // 竖屏手机保持用户配置
        assertEquals(480, AdaptiveLayoutPolicy.detailMinColumnWidth(412, 480))
        // 超宽屏仍按配置列宽排更多列
        assertEquals(480, AdaptiveLayoutPolicy.detailMinColumnWidth(1280, 480))
        // 用户配置本来就比一半还小则不放大
        assertEquals(280, AdaptiveLayoutPolicy.detailMinColumnWidth(776, 280))
    }

    @Test
    fun thumbGridColumnsGrowsWithWindowWidthButNeverShrinksConfiguredValue() {
        assertEquals(4, AdaptiveLayoutPolicy.thumbGridColumns(800, configuredColumns = 3))
        assertEquals(3, AdaptiveLayoutPolicy.thumbGridColumns(412, configuredColumns = 3))
        assertEquals(5, AdaptiveLayoutPolicy.thumbGridColumns(412, configuredColumns = 5))
        assertEquals(6, AdaptiveLayoutPolicy.thumbGridColumns(1280, configuredColumns = 3))
        assertEquals(1, AdaptiveLayoutPolicy.thumbGridColumns(120, configuredColumns = 1))
    }
}
