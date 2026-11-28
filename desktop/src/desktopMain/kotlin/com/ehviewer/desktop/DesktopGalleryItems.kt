package com.ehviewer.desktop

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.ehviewer.core.database.client.CATEGORY_ARTIST_CG
import com.ehviewer.core.database.client.CATEGORY_ASIAN_PORN
import com.ehviewer.core.database.client.CATEGORY_COSPLAY
import com.ehviewer.core.database.client.CATEGORY_DOUJINSHI
import com.ehviewer.core.database.client.CATEGORY_GAME_CG
import com.ehviewer.core.database.client.CATEGORY_IMAGE_SET
import com.ehviewer.core.database.client.CATEGORY_MANGA
import com.ehviewer.core.database.client.CATEGORY_MISC
import com.ehviewer.core.database.client.CATEGORY_NON_H
import com.ehviewer.core.database.client.CATEGORY_WESTERN
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 桌面端画廊视觉映射。分类底色与移动端 EhUtils.getCategoryColor 同值（桌面无 harmonize 设置，不做色调融合）。
object DesktopGalleryVisuals {
    fun categoryColor(category: Int): Color = Color(
        when (category) {
            CATEGORY_DOUJINSHI -> 0xfff44336u
            CATEGORY_MANGA -> 0xffff9800u
            CATEGORY_ARTIST_CG -> 0xfffbc02du
            CATEGORY_GAME_CG -> 0xff4caf50u
            CATEGORY_WESTERN -> 0xff8bc34au
            CATEGORY_NON_H -> 0xff2196f3u
            CATEGORY_IMAGE_SET -> 0xff3f51b5u
            CATEGORY_COSPLAY -> 0xff9c27b0u
            CATEGORY_ASIAN_PORN -> 0xff9575cdu
            CATEGORY_MISC -> 0xfff06292u
            else -> 0xff000000u
        }.toInt(),
    )

    // 与移动端 getCategoryTextColor 同规则：L* > 70 的浅色 chip 配深字，深色 chip 配浅字
    @Composable
    fun categoryTextColor(chipColor: Color): Color = if ((chipColor.luminance() > LSTAR_70_LUMINANCE) == (MiuixTheme.colorScheme.surface.luminance() < 0.5f)) {
        MiuixTheme.colorScheme.surface
    } else {
        MiuixTheme.colorScheme.onSurface
    }

    val favoriteIconColor = Color(0xFFFF3040)

    // L* = 70 对应的相对亮度（Y = ((70 + 16) / 116)^3）
    private const val LSTAR_70_LUMINANCE = 0.4076f
}

private const val MIN_THUMB_RATIO = 0.5f
private const val MAX_THUMB_RATIO = 1.5f
private const val DEFAULT_THUMB_RATIO = 0.67f

// 缩略图元数据缺失（0）时回退默认比例；元数据命中则钳制在 0.5~1.5（与移动端 GalleryInfoGridItem 同规则）
internal fun resolvedThumbRatio(thumbWidth: Int, thumbHeight: Int): Float = if (thumbHeight != 0) {
    (thumbWidth.toFloat() / thumbHeight).coerceIn(MIN_THUMB_RATIO, MAX_THUMB_RATIO)
} else {
    DEFAULT_THUMB_RATIO
}
