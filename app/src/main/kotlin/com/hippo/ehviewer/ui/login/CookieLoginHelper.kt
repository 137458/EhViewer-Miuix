package com.hippo.ehviewer.ui.login

data class ParsedIdentityCookies(
    val memberId: String?,
    val passHash: String?,
    val igneous: String?,
) {
    val isValid: Boolean get() = !memberId.isNullOrBlank() && !passHash.isNullOrBlank()
}

object CookieLoginHelper {
    const val KEY_IPB_MEMBER_ID = "ipb_member_id"
    const val KEY_IPB_PASS_HASH = "ipb_pass_hash"
    const val KEY_IGNEOUS = "igneous"

    fun parseCookieString(raw: String): ParsedIdentityCookies {
        if (raw.isBlank()) return ParsedIdentityCookies(null, null, null)

        val pairs = mutableMapOf<String, String>()
        val segments = raw.split(';', '\n')
        for (segment in segments) {
            val trimmed = segment.trim()
            if (trimmed.isEmpty()) continue
            val delimiter = when {
                '=' in trimmed -> '='
                ':' in trimmed -> ':'
                else -> continue
            }
            val parts = trimmed.split(delimiter, limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim().lowercase()
                val value = parts[1].trim().trim('"', '\'')
                if (key.isNotEmpty() && value.isNotEmpty()) {
                    pairs[key] = value
                }
            }
        }

        val memberId = pairs[KEY_IPB_MEMBER_ID]
        val passHash = pairs[KEY_IPB_PASS_HASH]
        val igneous = pairs[KEY_IGNEOUS]

        return ParsedIdentityCookies(
            memberId = memberId,
            passHash = passHash,
            igneous = igneous,
        )
    }

    fun validateAndFormat(memberId: String, passHash: String, igneous: String?): ParsedIdentityCookies? {
        val trimmedMemberId = memberId.trim()
        val trimmedPassHash = passHash.trim()
        val trimmedIgneous = igneous?.trim()?.ifEmpty { null }

        if (trimmedMemberId.isEmpty() || trimmedPassHash.isEmpty()) {
            return null
        }

        return ParsedIdentityCookies(
            memberId = trimmedMemberId,
            passHash = trimmedPassHash,
            igneous = trimmedIgneous,
        )
    }
}
