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
        val text = body ?: return@runCatching null
        val markerStart = text.indexOf(IMAGE_LIST_STRING)
        require(markerStart >= 0) { "imagelist marker missing" }
        val arrayStart = text.indexOf('[', markerStart + IMAGE_LIST_STRING.length)
        require(arrayStart >= 0) { "imagelist array missing" }
        val arrayEnd = findArrayEnd(text, arrayStart)
        val imagelist = text.substring(arrayStart, arrayEnd + 1)
        json.decodeFromString(ListSerializer(Item.serializer()), imagelist).map(Item::token)
    }.getOrNull()

    // 按括号配对定位数组结尾：不依赖尾随分号，也不被字符串值里的分号/括号干扰
    private fun findArrayEnd(text: String, arrayStart: Int): Int {
        var depth = 0
        var inString = false
        var escaped = false
        for (i in arrayStart until text.length) {
            val c = text[i]
            when {
                escaped -> escaped = false
                c == '\\' && inString -> escaped = true
                c == '"' -> inString = !inString
                inString -> Unit
                c == '[' -> depth++
                c == ']' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        throw IllegalArgumentException("unbalanced imagelist array")
    }

    @Serializable
    data class Item(@SerialName("k") val token: String)
}
