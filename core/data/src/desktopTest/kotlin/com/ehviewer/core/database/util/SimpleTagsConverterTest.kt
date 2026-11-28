package com.ehviewer.core.database.util

import kotlin.test.Test
import kotlin.test.assertEquals

// SIMPLE_TAGS 列的空值语义：空字符串代表「没有标签」，不能解出含空串的单元素列表，
// 否则标签展示与按标签过滤都会多出一个幽灵标签。
class SimpleTagsConverterTest {

    private val converter = SimpleTagsConverter()

    @Test
    fun blankStoredValueDecodesToEmptyList() {
        assertEquals(emptyList(), converter.fromString(""))
        assertEquals(emptyList(), converter.toString(emptyList()).let(converter::fromString))
    }

    @Test
    fun nonBlankValueRoundTrips() {
        val tags = listOf("favorites", "full color")
        assertEquals(tags, converter.fromString(converter.toString(tags)))
    }
}
