package com.ehviewer.desktop

import com.ehviewer.core.model.GalleryInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLEncoder

// 高级搜索开关位（与移动端 AdvanceTable 同值；SH=有命中页/STO=有种子/SFL=排除语言/SFU=排除上传者/SFT=排除标签）
object DesktopAdvanceSearchBits {
    const val SH = 0x1
    const val STO = 0x2
    const val SFL = 0x4
    const val SFU = 0x8
    const val SFT = 0x10
}

// 站点搜索过滤条件（与移动端 ListUrlBuilder 的 category/language/advanceSearch/minRating/pageFrom/pageTo 同语义）
@Serializable
data class DesktopSearchFilters(
    val category: Int = CATEGORY_ALL,
    val language: Int = -1,
    val advanceSearch: Int = 0,
    val minRating: Int = 0,
    val pageFrom: Int = 0,
    val pageTo: Int = 0,
) {
    val hasAdvanced: Boolean
        get() = advanceSearch > 0 || minRating > 0 || pageFrom > 0 || pageTo > 0

    companion object {
        const val CATEGORY_ALL = 0x3FF
        val DEFAULT = DesktopSearchFilters()
    }
}

// 过滤条件持久化编解码（存于 DesktopSettings；坏数据容错回默认值，未知键忽略以便前向兼容）
object DesktopSearchFiltersCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun decode(raw: String): DesktopSearchFilters =
        if (raw.isBlank()) {
            DesktopSearchFilters.DEFAULT
        } else {
            try {
                json.decodeFromString<DesktopSearchFilters>(raw)
            } catch (_: SerializationException) {
                DesktopSearchFilters.DEFAULT
            } catch (_: IllegalArgumentException) {
                DesktopSearchFilters.DEFAULT
            }
        }

    fun encode(filters: DesktopSearchFilters): String = json.encodeToString(filters)
}

// 站点搜索页 URL 构造（桌面在线搜索）。
// 站点分页为游标双向：next = 当前结果末位 gid（往新画廊方向）；prev = gid（往更早画廊方向）；
// range = 随机跳页起点。空白查询且无任何过滤/翻页参数时返回 null 表示不发起搜索。
object DesktopSearchUrl {
    fun build(
        query: String,
        nextGid: Long? = null,
        backward: Boolean = false,
        range: Int = 0,
        filters: DesktopSearchFilters = DesktopSearchFilters.DEFAULT,
    ): String? = buildWithBase("https://e-hentai.org/", query, nextGid, backward, range, filters)

    fun buildForTab(
        tab: LibraryTab,
        query: String = "",
        nextGid: Long? = null,
        backward: Boolean = false,
        range: Int = 0,
        filters: DesktopSearchFilters = DesktopSearchFilters.DEFAULT,
        page: Int = 0,
    ): String = when (tab) {
        LibraryTab.Subscription -> buildWithBase(
            "https://e-hentai.org/watched",
            query,
            nextGid,
            backward,
            range,
            filters,
        ) ?: "https://e-hentai.org/watched"
        LibraryTab.Toplist -> {
            val base = "https://e-hentai.org/toplist.php?tl=11"
            if (page > 0) "$base&p=${page - 1}" else base
        }
        LibraryTab.Home -> buildWithBase(
            "https://e-hentai.org/",
            query,
            nextGid,
            backward,
            range,
            filters,
        ) ?: "https://e-hentai.org/"
        else -> "https://e-hentai.org/popular"
    }

    private fun buildWithBase(
        base: String,
        query: String,
        nextGid: Long?,
        backward: Boolean,
        range: Int,
        filters: DesktopSearchFilters,
    ): String? {
        val trimmed = query.trim()
        val params = mutableListOf<Pair<String, String>>()
        val category = filters.category
        if (category > 0 && category != DesktopSearchFilters.CATEGORY_ALL) {
            // f_cats 为排除位掩码：站点语义是「排除勾选之外的分类」
            params.add("f_cats" to (category.inv() and DesktopSearchFilters.CATEGORY_ALL).toString())
        }
        if (trimmed.isNotEmpty()) {
            params.add("f_search" to URLEncoder.encode(buildKeyword(trimmed, filters), "UTF-8"))
        }
        when {
            nextGid == null || nextGid < 0 -> Unit
            backward -> params.add("prev" to nextGid.toString())
            else -> params.add("next" to nextGid.toString())
        }
        if (range > 0) params.add("range" to range.toString())
        appendAdvanced(params, filters)
        if (params.isEmpty()) return null
        return base + "?" + params.joinToString("&") { (k, v) -> "$k=$v" }
    }

    // 语言过滤走关键词前缀（与移动端同形）：keyword 已带 language:/l:/gid: 时不重复注入
    private fun buildKeyword(trimmed: String, filters: DesktopSearchFilters): String {
        val language = filters.language
        if (language !in GalleryInfo.S_LANG_TAGS.indices ||
            "gid:" in trimmed || "l:" in trimmed || "language:" in trimmed
        ) {
            return trimmed
        }
        return "${GalleryInfo.S_LANG_TAGS[language]} $trimmed"
    }

    private fun appendAdvanced(params: MutableList<Pair<String, String>>, filters: DesktopSearchFilters) {
        if (!filters.hasAdvanced) return
        params.add("advsearch" to "1")
        if (filters.advanceSearch and DesktopAdvanceSearchBits.SH != 0) params.add("f_sh" to "on")
        if (filters.advanceSearch and DesktopAdvanceSearchBits.STO != 0) params.add("f_sto" to "on")
        if (filters.advanceSearch and DesktopAdvanceSearchBits.SFL != 0) params.add("f_sfl" to "on")
        if (filters.advanceSearch and DesktopAdvanceSearchBits.SFU != 0) params.add("f_sfu" to "on")
        if (filters.advanceSearch and DesktopAdvanceSearchBits.SFT != 0) params.add("f_sft" to "on")
        if (filters.minRating > 0) params.add("f_srdd" to filters.minRating.toString())
        if (filters.pageFrom > 0) params.add("f_spf" to filters.pageFrom.toString())
        if (filters.pageTo > 0) params.add("f_spt" to filters.pageTo.toString())
    }
}

object DesktopOnlinePagination {
    fun canNavigatePrev(cursorIndex: Int): Boolean = cursorIndex > 0
    fun pageDisplayNumber(cursorIndex: Int): Int = (cursorIndex + 1).coerceAtLeast(1)
}