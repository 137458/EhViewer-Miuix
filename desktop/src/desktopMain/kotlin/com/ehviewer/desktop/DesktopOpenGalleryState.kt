package com.ehviewer.desktop

import com.ehviewer.core.database.client.GalleryDetailUrlParser
import com.ehviewer.core.model.BaseGalleryInfo

data class GalleryParsedTarget(
    val gid: Long,
    val token: String,
)

enum class DesktopOpenGalleryError {
    EmptyInput,
    InvalidInput,
}

object DesktopOpenGalleryState {
    private val GID_TOKEN_REGEX = Regex("""^(\d{1,14})[\s/,]+([a-fA-F0-9]{10})$""")

    fun parseInput(rawInput: String?): GalleryParsedTarget? {
        if (rawInput.isNullOrBlank()) return null
        val trimmed = rawInput.trim()

        if (trimmed.contains("/g/")) {
            val urlResult = GalleryDetailUrlParser.parse(trimmed, strict = false)
            if (urlResult != null) {
                return GalleryParsedTarget(urlResult.gid, urlResult.token.lowercase())
            }
        }

        val match = GID_TOKEN_REGEX.find(trimmed)
        if (match != null) {
            val (gidStr, tokenStr) = match.destructured
            val gid = gidStr.toLongOrNull() ?: return null
            return GalleryParsedTarget(gid, tokenStr.lowercase())
        }

        return null
    }

    fun validate(rawInput: String?): DesktopOpenGalleryError? {
        if (rawInput.isNullOrBlank()) return DesktopOpenGalleryError.EmptyInput
        return if (parseInput(rawInput) == null) {
            DesktopOpenGalleryError.InvalidInput
        } else {
            null
        }
    }

    fun validateInput(rawInput: String?): String? {
        if (rawInput.isNullOrBlank()) {
            return "Please enter a gallery URL or GID/Token"
        }
        val target = parseInput(rawInput)
        return if (target == null) {
            "Invalid gallery URL or GID/Token"
        } else {
            null
        }
    }

    fun createGalleryInfo(target: GalleryParsedTarget, customTitle: String? = null): BaseGalleryInfo = BaseGalleryInfo(
        gid = target.gid,
        token = target.token,
        title = customTitle?.takeIf { it.isNotBlank() } ?: "Gallery ${target.gid}",
    )
}
