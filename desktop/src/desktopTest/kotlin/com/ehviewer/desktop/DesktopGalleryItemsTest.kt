package com.ehviewer.desktop

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.ehviewer.core.database.client.CATEGORY_DOUJINSHI
import com.ehviewer.core.database.client.CATEGORY_MANGA
import com.ehviewer.core.database.client.CATEGORY_NON_H
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class DesktopGalleryItemsTest {

    @Test
    fun thumbRatioFallsBackToDefaultWhenMetadataMissing() {
        // 边界：元数据未回填（占位卡）时回退默认 0.67
        assertEquals(0.67f, resolvedThumbRatio(0, 0))
        assertEquals(0.67f, resolvedThumbRatio(100, 0))
    }

    @Test
    fun thumbRatioClampsToUsableRange() {
        // 常规：竖版封面正常换算；边界：极端扁/长比例钳制到 0.5~1.5
        assertEquals(0.75f, resolvedThumbRatio(750, 1000))
        assertEquals(0.5f, resolvedThumbRatio(400, 2000))
        assertEquals(1.5f, resolvedThumbRatio(2000, 400))
    }

    @Test
    fun categoryColorsMatchMobilePalette() {
        // 与移动端 EhUtils 底色同值（红线 doujinshi / 橙线 manga / 蓝线 Non-H）
        assertEquals(0xFFF44336, DesktopGalleryVisuals.categoryColor(CATEGORY_DOUJINSHI).toArgbLong())
        assertEquals(0xFFFF9800, DesktopGalleryVisuals.categoryColor(CATEGORY_MANGA).toArgbLong())
        assertEquals(0xFF2196F3, DesktopGalleryVisuals.categoryColor(CATEGORY_NON_H).toArgbLong())
    }

    @Test
    fun unknownCategoryFallsBackToBlack() {
        // 未知/占位分类回退黑底（与移动端 BG_COLOR_UNKNOWN 同值）
        assertEquals(0xFF000000, DesktopGalleryVisuals.categoryColor(-1).toArgbLong())
        assertNotEquals(DesktopGalleryVisuals.categoryColor(CATEGORY_DOUJINSHI), DesktopGalleryVisuals.categoryColor(-1))
    }

    private fun Color.toArgbLong(): Long = toArgb().toLong() and 0xFFFFFFFFL
}
