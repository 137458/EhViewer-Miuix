package com.ehviewer.core.database.client

object GalleryDetailUrlParser {
    const val DOMAIN_EX = "exhentai.org"
    const val DOMAIN_E = "e-hentai.org"

    private val URL_STRICT_PATTERN = Regex(
        "https?://(?:${Regex.escape(DOMAIN_EX)}|${Regex.escape(DOMAIN_E)}(?:/lofi)?)/(?:g|mpv)/(\\d+)/([0-9a-f]{10})",
    )
    private val URL_PATTERN = Regex("(\\d+)/([0-9a-f]{10})(?:[^0-9a-f]|$)")

    fun parse(url: String?, strict: Boolean = true): Result? {
        url ?: return null
        val pattern = if (strict) URL_STRICT_PATTERN else URL_PATTERN
        return pattern.find(url)?.destructured?.let { (gid, token) ->
            // 超出 Long 范围的 gid 视为不可解析，不抛出 NumberFormatException
            gid.toLongOrNull()?.let { Result(it, token) }
        }
    }

    data class Result(val gid: Long, val token: String)
}
