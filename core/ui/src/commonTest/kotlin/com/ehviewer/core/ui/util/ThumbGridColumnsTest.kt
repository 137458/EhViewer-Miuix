package com.ehviewer.core.ui.util

import kotlin.test.Test
import kotlin.test.assertEquals

class ThumbGridColumnsTest {

    @Test
    fun `configured columns act as the minimum`() {
        // 窄容器自适应列数低于配置值时，取配置值
        assertEquals(3, WindowLayout.thumbGridColumns(300, 3))
        assertEquals(3, WindowLayout.thumbGridColumns(300, 3, minColumnWidthDp = 120))
    }

    @Test
    fun `wide containers add columns past the default floor`() {
        // 900dp / 200dp 下限 = 4 列
        assertEquals(4, WindowLayout.thumbGridColumns(900, 3))
    }

    @Test
    fun `preview floor adds columns sooner while dragging the divider wider`() {
        // 预览条用更小的单列下限：拖宽分隔条时优先加列而不是拉伸单元格
        assertEquals(5, WindowLayout.thumbGridColumns(614, 3, minColumnWidthDp = 120))
        assertEquals(3, WindowLayout.thumbGridColumns(358, 3, minColumnWidthDp = 120))
    }
}
