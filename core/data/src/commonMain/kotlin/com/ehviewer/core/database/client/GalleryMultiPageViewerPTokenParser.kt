package com.ehviewer.core.database.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

// MPV（多页查看器）页 pToken 解析（下沉自 app 模块，阅读链路多端共用）。
// 解析失败返回 null，由调用方决定异常语义。
object GalleryMultiPageViewerPTokenParser {
    private const val IMAGE_LIST_STRING = "var imagelist = "

    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    fun parse(body: String?): List<String>? = runCatching {
        val index = body!!.indexOf(IMAGE_LIST_STRING)
        val imagelist = body.substring(index + IMAGE_LIST_STRING.length, body.indexOf(";", index))
        json.decodeFromString(ListSerializer(Item.serializer()), imagelist).map(Item::token)
    }.getOrNull()

    @Serializable
    data class Item(@SerialName("k") val token: String)
}
