package com.ehviewer.core.shell.list

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
import com.ehviewer.core.database.client.CATEGORY_PRIVATE
import com.ehviewer.core.database.client.CATEGORY_WESTERN
import com.ehviewer.core.i18n.MR
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.from
import com.materialkolor.ktx.toColor
import dev.icerock.moko.resources.StringResource
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 画廊条目视觉常量（与 :app EhUtils 同值同规则，桌面/移动共享）。
// 分类底色、chip 文字色规则、harmonize 色调融合均自 EhUtils 移植。
object GalleryItemVisuals {

    // 收藏红心（与移动端 EhUtils.favoriteIconColor 同值）
    val favoriteIconColor: Color = Color(0xffff3040)

    fun staticCategoryColor(category: Int): Color = Color(
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

    // harmonize：取 src 色相 + 主题 primaryContainer 的 chroma/tone（移动端默认开启，视觉随主题融合）
    @Composable
    fun categoryColor(category: Int, harmonize: Boolean): Color {
        val primary = staticCategoryColor(category)
        if (!harmonize) return primary
        val primaryContainer = MiuixTheme.colorScheme.primaryContainer
        return remember(primaryContainer, primary) { mergeColor(primaryContainer, primary) }
    }

    // chip 文字取色（组合内）；规则见 useSurfaceForChipText
    @Composable
    fun categoryTextColor(chipColor: Color): Color = if (useSurfaceForChipText(chipColor, isSystemInDarkTheme())) {
        MiuixTheme.colorScheme.surface
    } else {
        MiuixTheme.colorScheme.onSurface
    }

    // 移动端规则：chip 亮度（L*>70 等效阈 0.4076）与当前深浅模式一致时用 surface 色文字，否则 onSurface
    fun useSurfaceForChipText(chipColor: Color, isDark: Boolean): Boolean = (chipColor.luminance() > 0.4076f) == isDark

    fun mergeColor(primaryContainer: Color, src: Color): Color {
        val fromHct = Hct.from(src)
        val toHct = Hct.from(primaryContainer)
        return Hct.from(fromHct.hue, toHct.chroma, toHct.tone).toColor()
    }

    // 分类显示名资源（base 为站点 token，与 Android 端 GalleryCategoryFilterStrip / 桌面 DesktopCategories 同源）
    fun categoryLabelRes(category: Int): StringResource = when (category) {
        CATEGORY_MISC -> MR.strings.misc
        CATEGORY_DOUJINSHI -> MR.strings.doujinshi
        CATEGORY_MANGA -> MR.strings.manga
        CATEGORY_ARTIST_CG -> MR.strings.artist_cg
        CATEGORY_GAME_CG -> MR.strings.game_cg
        CATEGORY_IMAGE_SET -> MR.strings.image_set
        CATEGORY_COSPLAY -> MR.strings.cosplay
        CATEGORY_ASIAN_PORN -> MR.strings.asian_porn
        CATEGORY_NON_H -> MR.strings.non_h
        CATEGORY_WESTERN -> MR.strings.western
        CATEGORY_PRIVATE -> MR.strings.desktop_category_private
        else -> MR.strings.desktop_category_unknown
    }
}
