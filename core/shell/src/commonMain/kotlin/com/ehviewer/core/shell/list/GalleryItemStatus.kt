package com.ehviewer.core.shell.list

// 画廊条目的宿主供给状态：移动端由 FavouriteStatusRouter/DownloadManager/EhDB 映射，
// 桌面端由本地收藏 DAO / 阅读进度存储映射；共享组件不感知数据源
data class GalleryItemStatus(
    val isFavorited: Boolean = false,
    val favoriteName: String? = null,
    val favoriteNote: String? = null,
    val isDownloaded: Boolean = false,
    val readProgress: Int = 0,
    val harmonizeColor: Boolean = true,
    val isInFavScene: Boolean = false,
) {
    companion object {
        val Empty = GalleryItemStatus()
    }
}
