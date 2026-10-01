package com.ehviewer.core.database.client

import com.ehviewer.core.util.unescapeXml

// 图片页 HTML 解析（下沉自 app 模块 GalleryPageParser，阅读链路多端共用的第一步）。
// 解析失败（无图片地址）返回 null，由调用方决定异常语义。
object GalleryPageParser {
    private val PATTERN_IMAGE_URL = Regex("<img[^>]*src=\"([^\"]+)\" style")
    private val PATTERN_SKIP_HATH_KEY = Regex("onclick=\"return nl\\('([^)]+)'\\)")
    private val PATTERN_ORIGIN_IMAGE_URL = Regex("<a href=\"([^\"]+/fullimg/[^\"]+)\">")
    private val PATTERN_SHOW_KEY = Regex("var showkey=\"([0-9a-z]+)\";")

    fun parse(body: String): Result? {
        val imageUrl = PATTERN_IMAGE_URL.find(body)?.groupValues?.get(1)?.unescapeXml()
        return imageUrl?.let {
            Result(
                imageUrl = it,
                skipHathKey = PATTERN_SKIP_HATH_KEY.find(body)?.groupValues?.get(1),
                originImageUrl = PATTERN_ORIGIN_IMAGE_URL.find(body)?.groupValues?.get(1)?.unescapeXml(),
                showKey = PATTERN_SHOW_KEY.find(body)?.groupValues?.get(1),
            )
        }
    }

    data class Result(
        val imageUrl: String,
        val skipHathKey: String?,
        val originImageUrl: String?,
        val showKey: String?,
    )
}
