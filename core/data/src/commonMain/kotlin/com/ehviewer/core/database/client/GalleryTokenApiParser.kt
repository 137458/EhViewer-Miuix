package com.ehviewer.core.database.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

// Gallery Token API 解析（tokenlist → 首个 token）。
// 失败（空列表/畸形 JSON）返回 null，由调用方决定错误语义。
object GalleryTokenApiParser {
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    @Serializable
    data class Result(@SerialName("tokenlist") val tokenList: List<Item>)

    @Serializable
    data class Item(val gid: Long, val token: String)

    fun parse(body: String): String? = runCatching {
        json.decodeFromString<Result>(body).tokenList.firstOrNull()?.token
    }.getOrNull()
}
