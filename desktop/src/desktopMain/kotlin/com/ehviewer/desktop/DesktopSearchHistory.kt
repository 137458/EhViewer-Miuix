package com.ehviewer.desktop

object DesktopSearchHistory {
    const val DEFAULT_MAX_ITEMS = 8

    private fun sanitizeQuery(raw: String): String = raw.replace(Regex("[\\r\\n]+"), " ").trim()

    /**
     * 将搜索历史列表序列化为多行字符串存储
     */
    fun encode(history: List<String>): String = history.map { sanitizeQuery(it) }
        .filter { it.isNotEmpty() }
        .joinToString("\n")

    /**
     * 从持久化字符串中反序列化搜索历史列表，修剪空白、去除空项并忽略大小写去重
     */
    fun decode(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.lineSequence()
            .map { sanitizeQuery(it) }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .toList()
    }

    /**
     * 记录新搜索词：
     * - trim 前后空白并清洗内部换行，空白则不改变原列表
     * - 大小写不敏感去重：若列表中已有相同词（忽略大小写），剔除旧词并将新词置顶
     * - 最多保留 maxItems 条记录
     */
    fun addQuery(
        current: List<String>,
        newQuery: String,
        maxItems: Int = DEFAULT_MAX_ITEMS,
    ): List<String> {
        val sanitized = sanitizeQuery(newQuery)
        if (sanitized.isEmpty()) return current
        val filtered = current.filterNot { it.equals(sanitized, ignoreCase = true) }
        return (listOf(sanitized) + filtered).take(maxItems)
    }

    /**
     * 删除单条搜索历史（大小写不敏感匹配）
     */
    fun removeQuery(current: List<String>, target: String): List<String> {
        val sanitized = sanitizeQuery(target)
        if (sanitized.isEmpty()) return current
        return current.filterNot { it.equals(sanitized, ignoreCase = true) }
    }

    /**
     * 清空全部搜索历史
     */
    fun clearAll(): List<String> = emptyList()

    /**
     * 筛选搜索建议：
     * - 若 query 为空白，返回前 maxSuggestions 条最近搜索词
     * - 若 query 非空白，返回包含 query（忽略大小写）且不等于 query 自身的前 maxSuggestions 条
     */
    fun filterSuggestions(
        history: List<String>,
        query: String,
        maxSuggestions: Int = 5,
    ): List<String> {
        val sanitized = sanitizeQuery(query)
        return if (sanitized.isEmpty()) {
            history.take(maxSuggestions)
        } else {
            history.filter {
                it.contains(sanitized, ignoreCase = true) && !it.equals(sanitized, ignoreCase = true)
            }.take(maxSuggestions)
        }
    }
}
