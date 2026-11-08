package com.hippo.ehviewer.legacy

import com.ehviewer.core.files.read
import com.hippo.ehviewer.spider.SpiderInfo
import kotlinx.io.EOFException
import kotlinx.io.Source
import okio.Path

private const val VERSION_PREFIX = "VERSION"
private const val VERSION_V2 = 2

/** 头部字段（版本、gid、token、计数）本就是短行；上限用于防止损坏文件把内存吃满。 */
private const val MAX_HEADER_LINE_CHARS = 8192

/** 单行 pToken 允许的最大长度（含索引）。 */
private const val MAX_PTOKEN_LINE_CHARS = 2048

/** 单个 pToken 允许的最大字符数，超出视为损坏行。 */
private const val MAX_PTOKEN_CHARS = 1024

/** 本地文件声明的页数上限，防止损坏的 spider_info 触发超大分配。 */
private const val MAX_PAGES = 100_000

private class LegacySpiderInfoFormatException : Exception()

fun Path.readLegacySpiderInfo(): SpiderInfo? = runCatching {
    read { parseLegacySpiderInfo(this) }
}.getOrNull()

/**
 * 解析旧版文本格式的 spider_info。
 *
 * 兼容 VERSION2 与无版本头的 v1 文件；头部字段与 pToken 行都按上游语义做了长度与页数上限，
 * 单行损坏只跳过该行，结构性损坏（超长行、缺字段、页数越界、未知版本）返回 null。
 */
internal fun parseLegacySpiderInfo(source: Source): SpiderInfo? = runCatching {
    val first = source.readLineCapped(MAX_HEADER_LINE_CHARS) ?: return null
    val version = if (first.startsWith(VERSION_PREFIX)) {
        first.removePrefix(VERSION_PREFIX).trim().toIntOrNull() ?: return null
    } else {
        1
    }
    if (version != 1 && version != VERSION_V2) return null

    if (version == VERSION_V2) {
        source.readLineCapped(MAX_HEADER_LINE_CHARS) ?: return null // startPage
    }
    val gid = (source.readLineCapped(MAX_HEADER_LINE_CHARS) ?: return null).toLongOrNull() ?: return null
    val token = source.readLineCapped(MAX_HEADER_LINE_CHARS) ?: return null
    source.readLineCapped(MAX_HEADER_LINE_CHARS) ?: return null // mode
    source.readLineCapped(MAX_HEADER_LINE_CHARS) ?: return null // previewPages
    val previewPerPage = if (version == VERSION_V2) {
        (source.readLineCapped(MAX_HEADER_LINE_CHARS) ?: return null).toIntOrNull() ?: return null
    } else {
        -1
    }
    val pages = (source.readLineCapped(MAX_HEADER_LINE_CHARS) ?: return null).toIntOrNull() ?: return null
    if (pages <= 0 || pages > MAX_PAGES) return null

    val info = SpiderInfo(gid, token, pages, previewPerPage = previewPerPage)
    for (ignored in 0 until pages) {
        val line = source.readLineCapped(MAX_PTOKEN_LINE_CHARS) ?: break
        val (index, pToken) = parsePTokenLine(line) ?: continue
        info.pTokenMap[index] = pToken
    }
    info
}.getOrNull()

private fun parsePTokenLine(line: String): Pair<Int, String>? {
    val pos = line.indexOf(' ')
    if (pos <= 0) return null
    val index = line.substring(0, pos).toIntOrNull() ?: return null
    val pToken = line.substring(pos + 1)
    if (pToken.isEmpty() || pToken.length > MAX_PTOKEN_CHARS) return null
    return index to pToken
}

/**
 * 逐字节读到换行；到达 [maxChars] 视为结构损坏，抛出后由外层统一降级为 null。
 * 流结束且当前行为空时返回 null，表示没有更多行。
 */
private fun Source.readLineCapped(maxChars: Int): String? {
    val builder = StringBuilder()
    while (true) {
        if (builder.length > maxChars) throw LegacySpiderInfoFormatException()
        val byte = try {
            readByte().toInt()
        } catch (_: EOFException) {
            return builder.takeIf { it.isNotEmpty() }?.toString()
        }
        if (byte == '\n'.code) return builder.toString()
        if (byte != '\r'.code) builder.append(byte.toChar())
    }
}
