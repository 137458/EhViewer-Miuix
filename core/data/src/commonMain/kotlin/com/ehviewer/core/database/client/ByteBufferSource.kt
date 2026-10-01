package com.ehviewer.core.database.client

import io.ktor.util.moveToByteArray
import java.nio.ByteBuffer
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray

// Rust native 解析桥辅助层（下沉自 app 模块 ByteBufferSource）：
// native 解析器向缓冲区写入 CBOR 并返回结束位置，本函数重置 limit 后解码为模型。
inline fun <reified T> unmarshalParsingAs(body: ByteBuffer, parser: (ByteBuffer, Int) -> Int): T {
    val cborBytes = parser(body, body.limit())
    body.limit(cborBytes)
    val array = body.moveToByteArray()
    return Cbor.decodeFromByteArray<T>(array)
}
