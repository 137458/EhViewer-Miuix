package com.ehviewer.desktop

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ehviewer.core.ui.util.AdaptiveBreakpoints
import com.ehviewer.core.ui.util.NavigationChrome
import com.ehviewer.core.ui.util.WindowLayout
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopLayoutPolicyTest {

    @Test
    fun windowMinimumSizeStaysAtBreakpointFloor() {
        // 窗口最小尺寸与共享 Rail 断点对齐；Auto 导航形态由 WindowLayout.navigationChrome 判定
        assertEquals(600, DesktopLayoutPolicy.WINDOW_MIN_WIDTH_DP)
        assertEquals(500, DesktopLayoutPolicy.WINDOW_MIN_HEIGHT_DP)
        assertEquals(600, AdaptiveBreakpoints.RAIL_MIN_WIDTH_DP)
        assertEquals(600, AdaptiveBreakpoints.RAIL_MIN_HEIGHT_DP)
    }

    @Test
    fun autoNavigationChromeFallsBackToBottomBarOnShortWindows() {
        // Auto 语义（与移动端同源）：宽高双门槛 ≥600 才用 Rail，矮窗口落回悬浮底栏
        assertEquals(NavigationChrome.Rail, WindowLayout(widthDp = 1280, heightDp = 800).navigationChrome)
        assertEquals(NavigationChrome.Rail, WindowLayout(widthDp = 600, heightDp = 600).navigationChrome)
        assertEquals(NavigationChrome.BottomBar, WindowLayout(widthDp = 1280, heightDp = 500).navigationChrome)
        assertEquals(NavigationChrome.BottomBar, WindowLayout(widthDp = 600, heightDp = 500).navigationChrome)
    }

    @Test
    fun contentMaxWidthFollowsSharedBreakpointLadder() {
        // 限宽档走共享阶梯：≥840 用 840dp，600~839 用 640dp，窄窗不限
        assertEquals(840.dp, WindowLayout(widthDp = 1280, heightDp = 800).contentMaxWidth)
        assertEquals(640.dp, WindowLayout(widthDp = 720, heightDp = 800).contentMaxWidth)
        assertEquals(null as Dp?, WindowLayout(widthDp = 599, heightDp = 800).contentMaxWidth)
    }
}
