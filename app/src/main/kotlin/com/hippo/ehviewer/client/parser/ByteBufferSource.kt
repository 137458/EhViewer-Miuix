package com.hippo.ehviewer.client.parser

import com.ehviewer.core.database.client.unmarshalParsingAs as CoreUnmarshalParsingAs
import java.nio.ByteBuffer

// 下沉共享层后的兼容转发（Rust native 桥辅助），app 内既有调用点无需改动
inline fun <reified T> unmarshalParsingAs(body: ByteBuffer, parser: (ByteBuffer, Int) -> Int): T = CoreUnmarshalParsingAs(body, parser)
