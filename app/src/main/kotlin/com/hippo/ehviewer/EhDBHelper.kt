package com.hippo.ehviewer

fun shouldDeleteGallery(
    inDownloads: Boolean,
    inLocalFavorites: Boolean,
    inHistory: Boolean,
): Boolean = !inDownloads && !inLocalFavorites && !inHistory

private val FTS_TOKEN_SEPARATOR = Regex("\\s+")

/**
 * Builds a safe FTS4 MATCH expression out of a user supplied search keyword.
 *
 * The keyword used to be interpolated directly into `MATCH '*keyword*'`, so quotes, asterisks or
 * any other FTS operator in the input made SQLite fail the statement with a syntax error. Each
 * token is quoted (embedded quotes doubled) and turned into a prefix query; tokens without any
 * letter or digit are dropped since they cannot form a searchable term.
 *
 * @return the MATCH expression, or null when the keyword contains no usable token.
 */
internal fun buildFtsQuery(keyword: String): String? {
    val tokens = keyword.split(FTS_TOKEN_SEPARATOR).filter { token -> token.any { it.isLetterOrDigit() } }
    if (tokens.isEmpty()) return null
    return tokens.joinToString(separator = " ") { token -> "\"" + token.replace("\"", "\"\"") + "\"*" }
}
