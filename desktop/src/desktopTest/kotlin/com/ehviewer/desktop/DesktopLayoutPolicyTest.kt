package com.ehviewer.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopLayoutPolicyTest {

    @Test
    fun wideScreenBreakpointMatchesSixHundredDp() {
        // 边界：599.9 窄屏 / 600 整宽屏（与窗口最小宽对齐，桌面恒为宽屏形态）
        assertFalse(DesktopLayoutPolicy.isWideScreen(599.9f))
        assertTrue(DesktopLayoutPolicy.isWideScreen(600f))
        assertTrue(DesktopLayoutPolicy.isWideScreen(1280f))
    }

    @Test
    fun degenerateWidthsAreNarrow() {
        assertFalse(DesktopLayoutPolicy.isWideScreen(0f))
        assertFalse(DesktopLayoutPolicy.isWideScreen(-1f))
    }

    @Test
    fun layoutConstantsAlignWithMiuixLargeScreenSpec() {
        // 对齐 MIUIX 规范第 4 节：内容限宽 760 / 网格卡片最小 180 / 窗口最小尺寸与断点一致
        assertEquals(600, DesktopLayoutPolicy.WIDE_SCREEN_MIN_WIDTH_DP)
        assertEquals(760, DesktopLayoutPolicy.CONTENT_MAX_WIDTH_DP)
        assertEquals(180, DesktopLayoutPolicy.GRID_CARD_MIN_WIDTH_DP)
        assertEquals(600, DesktopLayoutPolicy.WINDOW_MIN_WIDTH_DP)
        assertEquals(500, DesktopLayoutPolicy.WINDOW_MIN_HEIGHT_DP)
    }
}
