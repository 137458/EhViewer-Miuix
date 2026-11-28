package com.hippo.ehviewer.ui.tools

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

class DialogOffsetTest {
    private val safe = Rect(left = -100f, top = -200f, right = 100f, bottom = 200f)

    @Test
    fun `clamps offset to nearest edge`() {
        val clamped = Offset(500f, 0f).safeOffset(safe)
        assertEquals(100f, clamped.x, 0.001f)
        assertEquals(0f, clamped.y, 0.001f)
    }

    // safeOffset 只把气泡夹回最近的一条边（单轴），不会同时对两轴求夹取
    @Test
    fun `clamps offset to left edge`() {
        val clamped = Offset(-900f, 0f).safeOffset(safe)
        assertEquals(-100f, clamped.x, 0.001f)
        assertEquals(0f, clamped.y, 0.001f)
    }

    @Test
    fun `clamps offset to bottom edge`() {
        val clamped = Offset(0f, 900f).safeOffset(safe)
        assertEquals(0f, clamped.x, 0.001f)
        assertEquals(200f, clamped.y, 0.001f)
    }

    @Test
    fun `degenerate safe area collapses to its center`() {
        val degenerate = Rect(50f, 50f, 50f, 50f)
        val clamped = Offset(500f, -500f).safeOffset(degenerate)
        assertEquals(50f, clamped.x, 0.001f)
        assertEquals(50f, clamped.y, 0.001f)
    }
}
