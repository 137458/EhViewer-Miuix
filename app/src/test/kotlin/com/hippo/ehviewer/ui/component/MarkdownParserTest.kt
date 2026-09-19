package com.hippo.ehviewer.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun testParseTableBlock() {
        val markdown = """
            | 文件名 | 类型 / 最低系统 | 说明与选择建议 |
            |:---|:---|:---|
            | `EhViewer-1.15.0-default-arm64-v8a.apk` | 主流安装包 (推荐)<br>Android 8.0+ | 专为近几年主流 64 位设备优化，包名后缀 `.miuix` |
            | `EhViewer-1.15.0-default-universal.apk` | 通用安装包<br>Android 8.0+ | 通用兜底包，当前工程下与 arm64-v8a 内容及体积完全一致 |
        """.trimIndent()

        val blocks = parseMarkdownBlocks(markdown)
        assertEquals(1, blocks.size)
        assertTrue("Expected Table block but was ${blocks.first()::class.simpleName}", blocks.first() is MarkdownBlock.Table)

        val table = blocks.first() as MarkdownBlock.Table
        assertEquals(listOf("文件名", "类型 / 最低系统", "说明与选择建议"), table.headers)
        assertEquals(2, table.rows.size)
        assertEquals(
            listOf(
                "`EhViewer-1.15.0-default-arm64-v8a.apk`",
                "主流安装包 (推荐)<br>Android 8.0+",
                "专为近几年主流 64 位设备优化，包名后缀 `.miuix`",
            ),
            table.rows[0],
        )
        assertEquals(
            listOf(
                "`EhViewer-1.15.0-default-universal.apk`",
                "通用安装包<br>Android 8.0+",
                "通用兜底包，当前工程下与 arm64-v8a 内容及体积完全一致",
            ),
            table.rows[1],
        )
    }

    @Test
    fun testBrTagConvertedToNewline() {
        val input = "主流安装包 (推荐)<br>Android 8.0+<br/>兼容版本<br />测试"
        val annotated = buildAnnotatedContent(input)
        val expected = "主流安装包 (推荐)\nAndroid 8.0+\n兼容版本\n测试"
        assertEquals(expected, annotated.text)
    }

    @Test
    fun testTableWithSurroundingContent() {
        val markdown = """
            ## 产物说明与下载指南

            | 文件名 | 说明 |
            |:---|:---|
            | arm64 | 64位优化 |

            ---

            ## 新增
            - 支持了全新功能
        """.trimIndent()

        val blocks = parseMarkdownBlocks(markdown)
        assertEquals(5, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Heading)
        assertEquals(2, (blocks[0] as MarkdownBlock.Heading).level)
        assertEquals("产物说明与下载指南", (blocks[0] as MarkdownBlock.Heading).text)

        assertTrue(blocks[1] is MarkdownBlock.Table)
        val table = blocks[1] as MarkdownBlock.Table
        assertEquals(listOf("文件名", "说明"), table.headers)
        assertEquals(listOf(listOf("arm64", "64位优化")), table.rows)

        assertTrue(blocks[2] is MarkdownBlock.Divider)

        assertTrue(blocks[3] is MarkdownBlock.Heading)
        assertEquals(2, (blocks[3] as MarkdownBlock.Heading).level)
        assertEquals("新增", (blocks[3] as MarkdownBlock.Heading).text)

        assertTrue(blocks[4] is MarkdownBlock.BulletItem)
        assertEquals("支持了全新功能", (blocks[4] as MarkdownBlock.BulletItem).text)
    }

    @Test
    fun tableColumnsOverflowNarrowContainerSoRowsCanScrollHorizontally() {
        val headers = listOf("文件名", "系统", "说明")
        val rows = listOf(
            listOf("EhViewer-1.15.0-default-arm64-v8a.apk", "Android 8.0+", "专为 64 位设备优化推荐版本"),
        )
        val spacingDp = 8f
        val availableWidthDp = 300f

        val widths = computeTableColumnWidths(headers, rows, availableWidthDp, spacingDp)

        assertEquals(3, widths.size)
        // 容器放不下时总宽必须溢出，交给横向滚动兜底，而不是把列压到不可读
        assertTrue(
            "总宽 ${widths.sum() + spacingDp * 2} 未溢出容器 $availableWidthDp",
            widths.sum() + spacingDp * 2 > availableWidthDp,
        )
        // 列宽由内容长度决定：文本最长的列必须最宽
        assertTrue("最长文本列应最宽：$widths", widths[0] > widths[1])
    }

    @Test
    fun tableColumnsExpandToFillWideContainer() {
        val headers = listOf("文件", "说明")
        val rows = listOf(listOf("arm64", "64位"))
        val spacingDp = 8f
        val availableWidthDp = 700f

        val widths = computeTableColumnWidths(headers, rows, availableWidthDp, spacingDp)

        assertEquals(2, widths.size)
        // 容器够宽：列宽按权重撑满，横向不再需要滚动
        assertEquals(availableWidthDp, widths.sum() + spacingDp, 0.5f)
        assertTrue("列宽必须为正：$widths", widths.all { it > 0f })
    }

    @Test
    fun tableColumnsKeepMinimumWidthsWhenContainerExactlyFits() {
        val headers = listOf("文件", "说明")
        val rows = listOf(listOf("arm64", "64位"))
        val spacingDp = 8f

        val minimums = computeTableColumnWidths(headers, rows, availableWidthDp = 100f, spacingDp = spacingDp)
        val exact = computeTableColumnWidths(
            headers,
            rows,
            availableWidthDp = minimums.sum() + spacingDp,
            spacingDp = spacingDp,
        )

        // 恰好放下时保持最小列宽，不做伸展
        assertEquals(minimums, exact)
    }
}
