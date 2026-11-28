package com.ehviewer.core.database.util

import androidx.room.TypeConverter

class SimpleTagsConverter {
    // 空字符串代表「没有标签」：直接 split 会得到含空串的单元素列表
    @TypeConverter
    fun fromString(value: String) = if (value.isEmpty()) emptyList() else value.split(", ")

    @TypeConverter
    fun toString(value: List<String>) = value.joinToString()
}
