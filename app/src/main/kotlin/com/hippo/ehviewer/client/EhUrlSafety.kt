package com.hippo.ehviewer.client

import java.net.URI

private val SENSITIVE_QUERY_KEYS = setOf(
    "token",
    "hath",
    "key",
    "secret",
    "password",
    "pass",
    "sig",
    "signature",
    "hash",
    "session",
    "cookie",
    "igneous",
)

/**
 * Masks credential like query parameters so a URL can be logged without leaking them.
 */
internal fun redactUrlForLog(url: String): String {
    val queryStart = url.indexOf('?')
    if (queryStart < 0) return url
    val prefix = url.substring(0, queryStart + 1)
    val query = url.substring(queryStart + 1)
    val fragmentStart = query.indexOf('#')
    val fragment = if (fragmentStart < 0) "" else query.substring(fragmentStart)
    val parameters = if (fragmentStart < 0) query else query.substring(0, fragmentStart)
    val redacted = parameters.split('&').joinToString("&") { parameter ->
        val separator = parameter.indexOf('=')
        if (separator < 0) {
            parameter
        } else {
            val name = parameter.substring(0, separator)
            if (name.lowercase() in SENSITIVE_QUERY_KEYS) "$name=***" else parameter
        }
    }
    return prefix + redacted + fragment
}

private fun isEhHost(host: String): Boolean {
    val normalized = host.lowercase()
    return normalized == EhUrl.DOMAIN_E || normalized == EhUrl.DOMAIN_EX ||
        normalized.endsWith(".${EhUrl.DOMAIN_E}") || normalized.endsWith(".${EhUrl.DOMAIN_EX}")
}

internal fun isEhUrl(url: String): Boolean = runCatching {
    val uri = URI(url)
    uri.scheme?.lowercase() in setOf("http", "https") && uri.host?.let(::isEhHost) == true
}.getOrDefault(false)

/**
 * Resolves a redirect target against the request URL and rejects anything that leaves E-Hentai,
 * so a malicious or broken [location] cannot redirect the client to an arbitrary host.
 */
internal fun resolveRedirectUrl(baseUrl: String, location: String?): String? {
    val target = location?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val resolved = runCatching { URI(baseUrl).resolve(target) }.getOrNull() ?: return null
    return resolved.toString().takeIf(::isEhUrl)
}
