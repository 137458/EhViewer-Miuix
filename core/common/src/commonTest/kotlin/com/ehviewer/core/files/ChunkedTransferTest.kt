package com.ehviewer.core.files

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// 大文件复制的分块驱动：sendfile/transferTo 单次调用有内核上限，超限会被截断
class ChunkedTransferTest {
    private data class TransferCall(val offset: Long, val size: Long)

    @Test
    fun splitsLargeTransferIntoChunksUnderKernelLimit() {
        val chunk = MAX_TRANSFER_CHUNK
        val total = chunk * 3 + 123
        val calls = ArrayList<TransferCall>()
        val copied = transferInChunks(total, chunk) { offset, size ->
            calls += TransferCall(offset, size)
            size
        }
        assertEquals(4, calls.size)
        assertEquals(listOf(0L, chunk, chunk * 2, chunk * 3), calls.map { it.offset })
        assertEquals(listOf(chunk, chunk, chunk, 123L), calls.map { it.size })
        assertEquals(total, copied)
    }

    @Test
    fun unboundedTotalNeverExceedsChunkLimit() {
        val chunk = MAX_TRANSFER_CHUNK
        val calls = ArrayList<TransferCall>()
        val copied = transferInChunks(Long.MAX_VALUE, chunk) { offset, size ->
            calls += TransferCall(offset, size)
            if (calls.size == 3) 0L else size
        }
        assertEquals(3, calls.size)
        assertTrue(calls.all { it.size <= chunk }, "单次传输不得超过内核上限: $calls")
        assertEquals(chunk * 2, copied)
    }

    @Test
    fun accumulatesShortTransfersWithoutLosingBytes() {
        val calls = ArrayList<TransferCall>()
        val copied = transferInChunks(25L, 100L) { offset, size ->
            calls += TransferCall(offset, size)
            minOf(size, 10L)
        }
        assertEquals(listOf(0L, 10L, 20L), calls.map { it.offset })
        assertEquals(listOf(25L, 15L, 5L), calls.map { it.size })
        assertEquals(25L, copied)
    }

    @Test
    fun stopsWhenTransferReportsEndOfInput() {
        assertEquals(0L, transferInChunks(100L, 100L) { _, _ -> 0L })
    }

    @Test
    fun rejectsNonPositiveChunkLimit() {
        assertFailsWith<IllegalArgumentException> {
            transferInChunks(10L, 0L) { _, size -> size }
        }
    }
}
