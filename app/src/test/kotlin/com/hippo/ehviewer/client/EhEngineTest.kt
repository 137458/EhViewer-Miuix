package com.hippo.ehviewer.client

import io.ktor.utils.io.ByteReadChannel
import java.nio.ByteBuffer
import java.util.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking

class EhEngineTest {

    @Test
    fun readToByteBuffer_smallPayload_fitsInInitialBuffer() {
        runBlocking {
            val data = ByteArray(1024) { it.toByte() }
            val channel = ByteReadChannel(data)
            val initialBuffer = ByteBuffer.allocateDirect(INITIAL_BUFFER_SIZE)

            val result = channel.readToByteBuffer(initialBuffer)
            result.flip()

            assertEquals(1024, result.remaining())
            val output = ByteArray(result.remaining())
            result.get(output)
            assertContentEquals(data, output)
        }
    }

    @Test
    fun readToByteBuffer_payloadExceedingInitialBuffer_expandsBufferAndReadsAll() {
        runBlocking {
            // 600KB payload > 512KB initial buffer
            val size = 600 * 1024
            val data = ByteArray(size)
            Random(42).nextBytes(data)
            val channel = ByteReadChannel(data)
            val initialBuffer = ByteBuffer.allocateDirect(INITIAL_BUFFER_SIZE)

            val result = channel.readToByteBuffer(initialBuffer)
            result.flip()

            assertEquals(size, result.remaining())
            val output = ByteArray(result.remaining())
            result.get(output)
            assertContentEquals(data, output)
        }
    }

    @Test
    fun readToByteBuffer_payloadExceedingMaxSize_throwsIllegalStateException() {
        runBlocking {
            val size = 2 * 1024 * 1024
            val data = ByteArray(size)
            val channel = ByteReadChannel(data)
            val initialBuffer = ByteBuffer.allocateDirect(512 * 1024)

            // Set maxSize to 1MB, so 2MB payload must throw
            assertFailsWith<IllegalStateException> {
                channel.readToByteBuffer(initialBuffer, maxSize = 1024 * 1024)
            }
        }
    }
}
