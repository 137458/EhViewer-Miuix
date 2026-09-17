package com.hippo.ehviewer.spider

import kotlin.test.Test
import kotlin.test.assertEquals

class SpiderQueenBackoffTest {

    @Test
    fun `calculateBackoffDelay follows strict exponential growth`() {
        // retry 0: 500ms
        assertEquals(500L, calculateBackoffDelay(0))
        // retry 1: 1000ms
        assertEquals(1000L, calculateBackoffDelay(1))
        // retry 2: 2000ms
        assertEquals(2000L, calculateBackoffDelay(2))
        // retry 3: 4000ms
        assertEquals(4000L, calculateBackoffDelay(3))
        // retry 4: 8000ms
        assertEquals(8000L, calculateBackoffDelay(4))
    }

    @Test
    fun `calculateBackoffDelay caps at maximum configured delay`() {
        // High retry count should not overflow or exceed max delay
        assertEquals(8000L, calculateBackoffDelay(5))
        assertEquals(8000L, calculateBackoffDelay(10))
        assertEquals(8000L, calculateBackoffDelay(100))
    }

    @Test
    fun `calculateBackoffDelay handles negative retries defensively`() {
        assertEquals(500L, calculateBackoffDelay(-1))
    }
}
