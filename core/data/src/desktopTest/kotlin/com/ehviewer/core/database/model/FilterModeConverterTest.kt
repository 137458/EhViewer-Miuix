package com.ehviewer.core.database.model

import kotlin.test.Test
import kotlin.test.assertEquals

// FILTER.MODE 列的读取必须容忍历史/未来版本写入的越界值：
// 直接按序号索引会抛 ArrayIndexOutOfBoundsException，让整张过滤器表读不出来。
class FilterModeConverterTest {

    private val converter = FilterModeConverter()

    @Test
    fun knownValuesRoundTrip() {
        FilterMode.entries.forEach { assertEquals(it, converter.toFilterMode(converter.fromFilterMode(it))) }
    }

    @Test
    fun outOfRangeValueFallsBackInsteadOfThrowing() {
        assertEquals(FilterMode.TITLE, converter.toFilterMode(-1))
        assertEquals(FilterMode.TITLE, converter.toFilterMode(FilterMode.entries.size))
        assertEquals(FilterMode.TITLE, converter.toFilterMode(Int.MAX_VALUE))
    }
}
