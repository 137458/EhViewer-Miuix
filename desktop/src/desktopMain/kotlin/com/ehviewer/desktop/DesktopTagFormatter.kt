package com.ehviewer.desktop

// 分享摘要的栏目标签包：由界面层注入本地化文案，保持本对象无 i18n 依赖
data class ShareSummaryLabels(
    val url: String,
    val rating: String,
    val pages: String,
    val category: String,
    val tags: String,
    val none: String,
)

object DesktopTagFormatter {

    /**
     * 将包含逗号或单个标签的列表拆分成纯净标签列表，去重并保留首次出现的相对顺序
     */
    fun splitTags(rawTags: List<String>?): List<String> {
        if (rawTags.isNullOrEmpty()) return emptyList()
        val result = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        for (item in rawTags) {
            val pieces = item.split(',')
            for (p in pieces) {
                val trimmed = p.trim()
                if (trimmed.isNotEmpty()) {
                    val lower = trimmed.lowercase()
                    if (seen.add(lower)) {
                        result.add(trimmed)
                    }
                }
            }
        }
        return result
    }

    /**
     * 解析单个标签的 namespace 与名称：
     * - 若含有冒号，冒号前为 namespace，冒号后为 tag 名称；
     * - 若不含冒号，默认归入 "misc"
     */
    fun parseTagNamespace(tag: String): Pair<String, String> {
        val trimmed = tag.trim()
        val colonIndex = trimmed.indexOf(':')
        return if (colonIndex > 0) {
            val ns = trimmed.substring(0, colonIndex).trim()
            val name = trimmed.substring(colonIndex + 1).trim()
            ns to name
        } else {
            "misc" to trimmed
        }
    }

    /**
     * 按 namespace 对标签进行归类分组，保留原有顺序
     */
    fun groupTags(tags: List<String>?): Map<String, List<String>> {
        val cleanTags = splitTags(tags)
        if (cleanTags.isEmpty()) return emptyMap()

        val grouped = LinkedHashMap<String, MutableList<String>>()
        for (tag in cleanTags) {
            val (ns, name) = parseTagNamespace(tag)
            val list = grouped.getOrPut(ns) { mutableListOf() }
            if (name.isNotEmpty() && !list.contains(name)) {
                list.add(name)
            }
        }
        return grouped
    }

    /**
     * 将 namespace 与 tag 名称拼装为搜索查询词
     */
    fun formatTagQuery(namespace: String, name: String): String {
        val cleanNs = namespace.trim()
        val cleanName = name.trim()
        return if (cleanNs.isEmpty() || cleanNs.equals("misc", ignoreCase = true)) {
            cleanName
        } else {
            "$cleanNs:$cleanName"
        }
    }

    /**
     * 生成结构化画廊摘要（便于一键复制与分享）；类目显示名与栏目标签由调用方注入
     */
    fun generateShareSummary(
        title: String,
        gid: Long,
        token: String,
        rating: Float,
        pages: Int,
        categoryName: String,
        tags: List<String>?,
        labels: ShareSummaryLabels,
    ): String {
        val url = galleryWebUrl(gid, token)
        val score = DesktopRating.formatRatingScore(rating)
        val cleanTags = splitTags(tags)
        val tagsString = if (cleanTags.isNotEmpty()) cleanTags.joinToString(", ") else labels.none

        return buildString {
            appendLine(title)
            appendLine("${labels.url}: $url")
            appendLine("${labels.rating}: $score | ${labels.pages}: $pages | ${labels.category}: $categoryName")
            append("${labels.tags}: $tagsString")
        }
    }
}
