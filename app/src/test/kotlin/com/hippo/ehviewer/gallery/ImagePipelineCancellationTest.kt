package com.hippo.ehviewer.gallery

import com.hippo.ehviewer.spider.calculateAutoRetryDelay
import com.hippo.ehviewer.spider.calculatePreloadSlots
import com.hippo.ehviewer.spider.shouldAutoRetry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore

class ImagePipelineCancellationTest {

    @Test
    fun `calculatePreloadSlots reserves at least one slot for active page`() {
        // When max concurrent is 3 (default), preload gets 2, active page is guaranteed 1 reserved slot
        assertEquals(2, calculatePreloadSlots(3))
        // When max concurrent is 4, preload gets 3
        assertEquals(3, calculatePreloadSlots(4))
        // When max concurrent is 2, preload gets 1
        assertEquals(1, calculatePreloadSlots(2))
        // Defensive: when max concurrent is 1, preload gets 1
        assertEquals(1, calculatePreloadSlots(1))
    }

    @Test
    fun `two-tier semaphore allows active request immediately when preload fills its quota`() = runBlocking {
        val totalSlots = 3
        val preloadSlots = calculatePreloadSlots(totalSlots)

        val totalSemaphore = Semaphore(totalSlots)
        val preloadSemaphore = Semaphore(preloadSlots)

        // 2 preload jobs acquire their permits
        assertTrue(preloadSemaphore.tryAcquire(), "Preload job 1 acquires preload permit")
        assertTrue(totalSemaphore.tryAcquire(), "Preload job 1 acquires total permit")

        assertTrue(preloadSemaphore.tryAcquire(), "Preload job 2 acquires preload permit")
        assertTrue(totalSemaphore.tryAcquire(), "Preload job 2 acquires total permit")

        // 3rd preload job cannot acquire preload permit (quota filled)
        assertFalse(preloadSemaphore.tryAcquire(), "Preload job 3 blocked by preload quota")

        // Active request bypasses preload quota and can acquire the remaining reserved slot in totalSemaphore!
        assertTrue(totalSemaphore.tryAcquire(), "Active request immediately gets reserved slot in totalSemaphore")

        // Now totalSemaphore is at full capacity (3/3)
        assertFalse(totalSemaphore.tryAcquire(), "Total concurrency limit strictly respected")

        // Clean up permits
        totalSemaphore.release()
        totalSemaphore.release()
        totalSemaphore.release()
        preloadSemaphore.release()
        preloadSemaphore.release()
    }

    @Test
    fun `auto retry policy bounds retries and handles backoff`() {
        // Attempt 0: retry allowed
        assertTrue(shouldAutoRetry(0, maxRetries = 2))
        assertEquals(1000L, calculateAutoRetryDelay(0))

        // Attempt 1: retry allowed
        assertTrue(shouldAutoRetry(1, maxRetries = 2))
        assertEquals(2000L, calculateAutoRetryDelay(1))

        // Attempt 2: retries exhausted, requires manual retry
        assertFalse(shouldAutoRetry(2, maxRetries = 2))
        assertFalse(shouldAutoRetry(3, maxRetries = 2))
    }
}
