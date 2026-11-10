package com.ehviewer.desktop

import com.ehviewer.core.shell.list.GalleryItemVisuals
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc

object DesktopCategories {
    /**
     * 桌面端类目显示名：委托至 core:shell 的 [GalleryItemVisuals.categoryLabelRes] 并经 moko 本地化。
     * 非 Composable 语境（状态回调、分享摘要）可直呼；组合内亦可安全使用（资源查找无状态）。
     */
    fun displayName(category: Int): String = StringDesc.Resource(GalleryItemVisuals.categoryLabelRes(category)).localized()
}
