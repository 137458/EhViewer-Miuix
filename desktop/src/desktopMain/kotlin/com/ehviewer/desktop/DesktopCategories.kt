package com.ehviewer.desktop

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
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc

// 桌面端类目显示名：经 moko 资源本地化（base 为站点 token，与 Android 端 GalleryCategoryFilterStrip 同源）。
// 数据层仅保留 getCategoryName 的 token 名供过滤匹配，显示名一律走此处。
object DesktopCategories {
    fun displayName(category: Int): String = localized(
        when (category) {
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
        },
    )

    // 非 Composable 语境（状态回调、分享摘要）可直呼；组合内亦可安全使用（资源查找无状态）
    private fun localized(resource: StringResource): String = StringDesc.Resource(resource).localized()
}
