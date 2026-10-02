package com.ehviewer.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopPreviewSpriteTest {
    @Test
    fun cellLayoutScalesSpriteByDisplayHeight() {
        // 单元 100x150，显示高 300 → 缩放 2x：格宽 200、位移取负
        val layout = DesktopPreviewSprite.cellLayout(
            offsetX = 250,
            clipWidth = 100,
            clipHeight = 150,
            displayHeight = 300f,
        )
        assertEquals(-500f, layout!!.offsetX)
        assertEquals(200f, layout!!.width)
        assertEquals(300f, layout!!.height)
    }

    @Test
    fun cellLayoutAtOneToOne() {
        val layout = DesktopPreviewSprite.cellLayout(
            offsetX = 80,
            clipWidth = 80,
            clipHeight = 120,
            displayHeight = 120f,
        )
        assertEquals(-80f, layout!!.offsetX)
        assertEquals(80f, layout!!.width)
        assertEquals(120f, layout!!.height)
    }

    @Test
    fun degenerateClipFallsBackToSquare() {
        // clipWidth/Height 非正：回退方形布局（displayHeight 见方），避免除零
        val layout = DesktopPreviewSprite.cellLayout(
            offsetX = 40,
            clipWidth = 0,
            clipHeight = 0,
            displayHeight = 100f,
        )
        assertEquals(0f, layout!!.offsetX)
        assertEquals(100f, layout!!.width)
        assertEquals(100f, layout!!.height)
    }
}
