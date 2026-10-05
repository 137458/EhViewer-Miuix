package com.ehviewer.core.shell.list

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.ehviewer.core.database.client.CATEGORY_ARTIST_CG
import com.ehviewer.core.database.client.CATEGORY_ASIAN_PORN
import com.ehviewer.core.database.client.CATEGORY_COSPLAY
import com.ehviewer.core.database.client.CATEGORY_DOUJINSHI
import com.ehviewer.core.database.client.CATEGORY_GAME_CG
import com.ehviewer.core.database.client.CATEGORY_IMAGE_SET
import com.ehviewer.core.database.client.CATEGORY_MANGA
import com.ehviewer.core.database.client.CATEGORY_MISC
import com.ehviewer.core.database.client.CATEGORY_NON_H
import com.ehviewer.core.database.client.CATEGORY_UNKNOWN
import com.ehviewer.core.database.client.CATEGORY_WESTERN
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GalleryItemVisualsTest {

    @Test
    fun `静态分类底色与移动端 EhUtils 同值`() {
        assertEquals(0xfff44336.toInt(), GalleryItemVisuals.staticCategoryColor(CATEGORY_DOUJINSHI).toArgb())
        assertEquals(0xffff9800.toInt(), GalleryItemVisuals.staticCategoryColor(CATEGORY_MANGA).toArgb())
        assertEquals(0xfffbc02d.toInt(), GalleryItemVisuals.staticCategoryColor(CATEGORY_ARTIST_CG).toArgb())
        assertEquals(0xff4caf50.toInt(), GalleryItemVisuals.staticCategoryColor(CATEGORY_GAME_CG).toArgb())
        assertEquals(0xff8bc34a.toInt(), GalleryItemVisuals.staticCategoryColor(CATEGORY_WESTERN).toArgb())
        assertEquals(0xff2196f3.toInt(), GalleryItemVisuals.staticCategoryColor(CATEGORY_NON_H).toArgb())
        assertEquals(0xff3f51b5.toInt(), GalleryItemVisuals.staticCategoryColor(CATEGORY_IMAGE_SET).toArgb())
        assertEquals(0xff9c27b0.toInt(), GalleryItemVisuals.staticCategoryColor(CATEGORY_COSPLAY).toArgb())
        assertEquals(0xff9575cd.toInt(), GalleryItemVisuals.staticCategoryColor(CATEGORY_ASIAN_PORN).toArgb())
        assertEquals(0xfff06292.toInt(), GalleryItemVisuals.staticCategoryColor(CATEGORY_MISC).toArgb())
        assertEquals(0xff000000.toInt(), GalleryItemVisuals.staticCategoryColor(CATEGORY_UNKNOWN).toArgb())
    }

    @Test
    fun `未知类目回落黑色`() {
        assertEquals(0xff000000.toInt(), GalleryItemVisuals.staticCategoryColor(0).toArgb())
        assertEquals(0xff000000.toInt(), GalleryItemVisuals.staticCategoryColor(0xffffffff.toInt()).toArgb())
    }

    @Test
    fun `chip 文字色规则与移动端一致`() {
        // luminance > 0.4076 的浅 chip：深色模式配 surface（浅色环境同值为深字则取反）
        val lightGray = Color(0.9f, 0.9f, 0.9f)
        val darkGray = Color(0.1f, 0.1f, 0.1f)
        assertTrue(GalleryItemVisuals.useSurfaceForChipText(lightGray, isDark = true))
        assertFalse(GalleryItemVisuals.useSurfaceForChipText(lightGray, isDark = false))
        assertFalse(GalleryItemVisuals.useSurfaceForChipText(darkGray, isDark = true))
        assertTrue(GalleryItemVisuals.useSurfaceForChipText(darkGray, isDark = false))
    }

    @Test
    fun `harmonize 融合保持色相仅改色调`() {
        // mergeColor：取 src 色相 + primaryContainer 的 chroma/tone——非同色输入下不返回原色即视为生效
        val src = GalleryItemVisuals.staticCategoryColor(CATEGORY_DOUJINSHI)
        val container = GalleryItemVisuals.staticCategoryColor(CATEGORY_MANGA)
        val merged = GalleryItemVisuals.mergeColor(container, src)
        assertTrue(merged != src)
        assertTrue(merged != container)
    }
}
