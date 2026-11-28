package com.ehviewer.core.network

// RFC 6265 §5.1.3：域归一化为小写、去掉前导点；空值与单点视为无效
internal fun normalizeCookieDomain(domain: String?): String? {
    val trimmed = domain?.trim()?.lowercase() ?: return null
    val stripped = trimmed.removePrefix(".")
    return stripped.ifEmpty { null }
}

// RFC 6265 §5.1.4：路径缺省或非法时回落 "/"
internal fun normalizeCookiePath(path: String?): String {
    val raw = path?.trim().orEmpty()
    return if (raw.startsWith("/")) raw else "/"
}

// 域匹配：域属性缺省即 host-only（精确相等），否则 host 须等于该域或为其子域
internal fun cookieDomainMatches(cookieDomain: String, host: String, hostOnly: Boolean): Boolean {
    val normalizedHost = host.lowercase()
    if (hostOnly) return cookieDomain == normalizedHost
    if (cookieDomain == normalizedHost) return true
    return normalizedHost.endsWith(".$cookieDomain")
}

// RFC 6265 §5.1.4 路径匹配：请求路径须落在 Cookie 路径的子树内
internal fun cookiePathMatches(requestPath: String, cookiePath: String): Boolean {
    val path = normalizeCookiePath(requestPath)
    if (cookiePath == path) return true
    if (!path.startsWith(cookiePath)) return false
    return cookiePath.endsWith("/") || path[cookiePath.length] == '/'
}
