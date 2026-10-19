package com.ehviewer.desktop

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// 快速搜索条目（持久化于 DesktopSettings，与移动端 QuickSearch 表同语义的字段子集：名称 + 查询串）
@Serializable
data class DesktopQuickSearchItem(
    val id: Long,
    val name: String,
    val query: String,
    val position: Int,
)

// 快速搜索命名校验错误（BLANK=空名 / DUPLICATE=与既有条目重名）
enum class DesktopQuickSearchNameError { BLANK, DUPLICATE }

// 快速搜索列表的编解码与增删移操作（纯模型，供 UI 层组合）
object DesktopQuickSearch {
    fun validateName(list: List<DesktopQuickSearchItem>, name: String): DesktopQuickSearchNameError? = when {
        name.isBlank() -> DesktopQuickSearchNameError.BLANK
        list.any { it.name == name } -> DesktopQuickSearchNameError.DUPLICATE
        else -> null
    }

    fun decode(raw: String): List<DesktopQuickSearchItem> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return emptyList()
        return runCatching {
            json.decodeFromString<List<DesktopQuickSearchItem>>(trimmed)
        }.getOrDefault(emptyList())
    }

    fun encode(list: List<DesktopQuickSearchItem>): String = json.encodeToString(list)

    fun add(list: List<DesktopQuickSearchItem>, name: String, query: String): List<DesktopQuickSearchItem> =
        list + DesktopQuickSearchItem(
            id = (list.maxOfOrNull { it.id } ?: 0L) + 1,
            name = name,
            query = query,
            position = list.size,
        )

    fun remove(list: List<DesktopQuickSearchItem>, id: Long): List<DesktopQuickSearchItem> =
        list.filterNot { it.id == id }.mapIndexed { index, item -> item.copy(position = index) }

    // 与移动端 reorderable 回调同语义：移除 from 再插入 to；非法下标原样返回
    fun move(list: List<DesktopQuickSearchItem>, fromIndex: Int, toIndex: Int): List<DesktopQuickSearchItem> {
        if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) return list
        val mutable = list.toMutableList()
        mutable.add(toIndex, mutable.removeAt(fromIndex))
        return mutable.mapIndexed { index, item -> item.copy(position = index) }
    }

    private val json = Json { ignoreUnknownKeys = true }
}
