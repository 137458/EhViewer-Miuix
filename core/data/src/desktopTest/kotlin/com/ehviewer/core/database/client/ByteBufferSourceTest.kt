package com.ehviewer.core.database.client

import io.ktor.util.moveToByteArray
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.cbor.CborArray
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray

// 桌面侧 Rust 解析桥辅助层验证（native 解析输出 → CBOR → 模型）
class ByteBufferSourceTest {

    @Serializable
    @CborArray
    data class SampleResult(val value: String, val event: String?)

    @Test
    fun unmarshalDecodesCborFromBufferAndResetsLimit() {
        val expected = SampleResult("hello", null)
        val cborBytes = Cbor.encodeToByteArray(SampleResult.serializer(), expected)

        // 模拟 native 桥：absolute put 直写缓冲区（不移动 position），返回写入结束位置
        // native 侧通过 GetDirectBufferAddress 取数据，桥接层要求 direct 且 position == 0
        val buffer = ByteBuffer.allocateDirect(64)
        val parser: (ByteBuffer, Int) -> Int = { buf, _ ->
            cborBytes.forEachIndexed { i, b -> buf.put(i, b) }
            cborBytes.size
        }

        val result = unmarshalParsingAs<SampleResult>(buffer, parser)
        assertEquals(expected, result)
        // bridge 约定：解析后 limit 被重置为 CBOR 结束位置
        assertEquals(cborBytes.size, buffer.limit())
        val out = ByteArray(buffer.limit())
        buffer.rewind()
        buffer.get(out)
        assertEquals(cborBytes.toList(), out.toList())
    }

    // native 侧只能从 direct buffer 取地址；堆缓冲区与已移动的 position 必须被拒绝
    @Test
    fun rejectsHeapBuffer() {
        val buffer = ByteBuffer.allocate(64)
        val parser: (ByteBuffer, Int) -> Int = { _, _ -> 0 }
        assertFailsWith<IllegalArgumentException> { unmarshalParsingAs<SampleResult>(buffer, parser) }
    }

    @Test
    fun rejectsBufferWithNonZeroPosition() {
        val buffer = ByteBuffer.allocateDirect(64).put(byteArrayOf(1, 2, 3)).flip()
        buffer.position(1)
        val parser: (ByteBuffer, Int) -> Int = { _, _ -> 0 }
        assertFailsWith<IllegalArgumentException> { unmarshalParsingAs<SampleResult>(buffer, parser) }
    }

    // native 返回的结束位置超出缓冲区容量或为负，说明桥接结果不可信，必须抛出而不是越界读取
    @Test
    fun rejectsCborSizeBeyondCapacity() {
        val buffer = ByteBuffer.allocateDirect(16)
        val parser: (ByteBuffer, Int) -> Int = { _, _ -> 17 }
        assertFailsWith<IllegalStateException> { unmarshalParsingAs<SampleResult>(buffer, parser) }
    }

    @Test
    fun rejectsNegativeCborSize() {
        val buffer = ByteBuffer.allocateDirect(16)
        val parser: (ByteBuffer, Int) -> Int = { _, _ -> -1 }
        assertFailsWith<IllegalStateException> { unmarshalParsingAs<SampleResult>(buffer, parser) }
    }
}
