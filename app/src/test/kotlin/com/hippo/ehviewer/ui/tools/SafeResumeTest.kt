package com.hippo.ehviewer.ui.tools

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.yield

class SafeResumeTest {
    @Test
    fun resumesActiveContinuationWithValue() = runBlocking {
        assertEquals(42, suspendingValue { it.safeResume(42) })
    }

    @Test
    fun safeResumeAfterCompletionIsNoOp() = runBlocking {
        var captured: CancellableContinuation<Int>? = null
        val deferred = async {
            suspendCancellableCoroutine<Int> { cont ->
                captured = cont
                cont.safeResume(1)
            }
        }
        assertEquals(1, deferred.await())
        val cont = checkNotNull(captured)
        assertFalse(cont.isActive)
        // 未判活的 resume 会抛 IllegalStateException("Already resumed")，safeResume 必须静默丢弃
        cont.safeResume(2)
        assertEquals(1, deferred.await())
    }

    @Test
    fun safeResumeAfterCancelIsNoOp() = runBlocking {
        var captured: CancellableContinuation<Int>? = null
        val deferred = async {
            suspendCancellableCoroutine<Int> { cont -> captured = cont }
        }
        yield()
        val cont = checkNotNull(captured)
        deferred.cancel()
        yield()
        assertFalse(cont.isActive)
        cont.safeResume(3)
    }

    private suspend fun <T> suspendingValue(block: (CancellableContinuation<T>) -> Unit): T = suspendCancellableCoroutine { cont -> block(cont) }
}
