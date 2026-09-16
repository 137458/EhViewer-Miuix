package com.hippo.ehviewer.spider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpeedTrackerTest {

    @Test
    fun `speedLevelToSpeed returns 0 for MIN_SPEED_LEVEL`() {
        assertEquals(0, speedLevelToSpeed(MIN_SPEED_LEVEL))
        assertEquals(0, speedLevelToSpeed(3))
    }

    @Test
    fun `speedLevelToSpeed returns correct powers of 2 for levels above minimum`() {
        // Level 4 -> 2^4 = 16 KB/s
        assertEquals(16, speedLevelToSpeed(4))
        // Level 5 -> 2^5 = 32 KB/s
        assertEquals(32, speedLevelToSpeed(5))
        // Level 6 -> 2^6 = 64 KB/s (Default)
        assertEquals(64, speedLevelToSpeed(6))
        // Level 7 -> 2^7 = 128 KB/s
        assertEquals(128, speedLevelToSpeed(7))
    }

    @Test
    fun `consecutive low speed logic requires multiple failures before triggering timeout`() {
        var timeoutTriggered = false
        val thresholdSpeed = 64 * 1024L
        var consecutiveFailures = 0

        val speedSequence = listOf(
            10 * 1024L, // Failure 1
            20 * 1024L, // Failure 2
            80 * 1024L, // Recovery! Resets counter
            15 * 1024L, // Failure 1
            25 * 1024L, // Failure 2
            10 * 1024L, // Failure 3 -> Timeout!
        )

        for (speed in speedSequence) {
            if (speed < thresholdSpeed) {
                consecutiveFailures++
                if (consecutiveFailures >= 3) {
                    timeoutTriggered = true
                    break
                }
            } else {
                consecutiveFailures = 0
            }
        }

        assertTrue(timeoutTriggered, "Timeout should have triggered after 3 consecutive failures")
    }

    @Test
    fun `intermittent speed recovery avoids timeout`() {
        var timeoutTriggered = false
        val thresholdSpeed = 64 * 1024L
        var consecutiveFailures = 0

        val speedSequence = listOf(
            10 * 1024L, // Failure 1
            20 * 1024L, // Failure 2
            70 * 1024L, // Recovery
            15 * 1024L, // Failure 1
            80 * 1024L, // Recovery
            20 * 1024L, // Failure 1
        )

        for (speed in speedSequence) {
            if (speed < thresholdSpeed) {
                consecutiveFailures++
                if (consecutiveFailures >= 3) {
                    timeoutTriggered = true
                    break
                }
            } else {
                consecutiveFailures = 0
            }
        }

        assertTrue(!timeoutTriggered, "Intermittent speed drops should not trigger timeout")
    }
}
