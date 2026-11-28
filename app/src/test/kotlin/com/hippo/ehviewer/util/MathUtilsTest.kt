package com.hippo.ehviewer.util

import kotlin.test.Test
import kotlin.test.assertEquals

class MathUtilsTest {
    @Test
    fun `calculateFraction returns zero for an empty range`() {
        assertEquals(0f, calculateFraction(5f, 5f, 5f))
        assertEquals(0f, calculateFraction(-1f, -1f, 100f))
    }

    @Test
    fun `calculateFraction interpolates and clamps`() {
        assertEquals(0f, calculateFraction(0f, 10f, -3f))
        assertEquals(1f, calculateFraction(0f, 10f, 30f))
        assertEquals(0.5f, calculateFraction(0f, 10f, 5f))
    }
}
