package com.ehviewer.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "FILTER")
data class Filter(
    @ColumnInfo(name = "MODE")
    val mode: FilterMode,

    @ColumnInfo(name = "TEXT")
    var text: String,

    @ColumnInfo(name = "ENABLE")
    var enable: Boolean = true,

    @PrimaryKey
    @ColumnInfo(name = "_id")
    var id: Long? = null,
)

enum class FilterMode(val field: Int) {
    TITLE(0),
    UPLOADER(1),
    TAG(2),
    TAG_NAMESPACE(3),
    COMMENTER(4),
    COMMENT(5),
}

class FilterModeConverter {
    @TypeConverter
    fun fromFilterMode(mode: FilterMode) = mode.field

    // 历史/未来版本写入的越界值降级为 TITLE，避免整张 FILTER 表读取失败
    @TypeConverter
    fun toFilterMode(mode: Int) = FilterMode.entries.getOrNull(mode) ?: FilterMode.TITLE
}
