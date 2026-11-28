package com.ehviewer.core.database.client

import io.ktor.util.moveToByteArray
import java.nio.ByteBuffer
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray

// Rust native 解析桥辅助层（下沉自 app 模块 ByteBufferSource）：
// native 解析器向缓冲区写入 CBOR 并返回结束位置，本函数重置 limit 后解码为模型。
// native 侧通过 GetDirectBufferAddress 取数据，因此入参必须是 position == 0 的 direct buffer；
// 返回位置越界或为负说明桥接结果不可信，直接抛错而不是越界读取。
inline fun <reified T> unmarshalParsingAs(body: ByteBuffer, parser: (ByteBuffer, Int) -> Int): T {
    require(body.isDirect && body.position() == 0) {
        "native parser requires a direct ByteBuffer at position 0 (isDirect=${body.isDirect}, position=${body.position()})"
    }
    val capacity = body.capacity()
    val cborSize = parser(body, body.limit())
    check(cborSize in 0..capacity) {
        "native parser returned invalid CBOR size $cborSize (capacity=$capacity)"
    }
    body.limit(cborSize)
    val array = body.moveToByteArray()
    return Cbor.decodeFromByteArray<T>(array)
}
