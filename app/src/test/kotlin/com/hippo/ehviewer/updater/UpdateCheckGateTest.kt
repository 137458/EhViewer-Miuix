package com.hippo.ehviewer.updater

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdateCheckGateTest {
    private val day = 86_400L

    @Test
    fun `forced checks always pass the gate`() {
        assertTrue(
            UpdateCheckGate.shouldCheck(
                forceCheck = true,
                lastCheckEpochSeconds = 100L,
                intervalDays = 7,
                nowEpochSeconds = 100L,
            ),
        )
    }

    @Test
    fun `never interval blocks automatic checks`() {
        assertFalse(
            UpdateCheckGate.shouldCheck(
                forceCheck = false,
                lastCheckEpochSeconds = 0L,
                intervalDays = 0,
                nowEpochSeconds = 365 * day,
            ),
        )
    }

    @Test
    fun `does not check before the interval elapsed`() {
        val now = 10 * day

        assertFalse(UpdateCheckGate.shouldCheck(false, now - 6 * day, 7, now))
        assertFalse(UpdateCheckGate.shouldCheck(false, now - 7 * day, 7, now))
    }

    @Test
    fun `checks once the interval elapsed`() {
        val now = 10 * day

        assertTrue(UpdateCheckGate.shouldCheck(false, now - 8 * day, 7, now))
        assertTrue(UpdateCheckGate.shouldCheck(false, now - 2 * day, 1, now))
    }
}
