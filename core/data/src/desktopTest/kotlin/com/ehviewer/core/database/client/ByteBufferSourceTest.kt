package com.ehviewer.core.database.client

import io.ktor.util.moveToByteArray
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
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
        val buffer = ByteBuffer.allocate(64)
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
}
