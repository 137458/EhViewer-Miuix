package com.ehviewer.desktop

import com.ehviewer.core.model.BaseGalleryInfo

sealed interface DesktopWindowKind {
    data object Library : DesktopWindowKind
    data object Settings : DesktopWindowKind
    data class GalleryDetail(val gallery: BaseGalleryInfo) : DesktopWindowKind
}

data class ShellWindow(val id: Long, val kind: DesktopWindowKind = DesktopWindowKind.Library) {
    constructor(id: Long, isSettings: Boolean) : this(
        id = id,
        kind = if (isSettings) DesktopWindowKind.Settings else DesktopWindowKind.Library,
    )

    val isSettings: Boolean get() = kind is DesktopWindowKind.Settings
}

object DesktopWindowManager {
    fun windowTitle(kind: DesktopWindowKind): String = when (kind) {
        DesktopWindowKind.Library -> "EhViewer"
        DesktopWindowKind.Settings -> "EhViewer Settings"
        is DesktopWindowKind.GalleryDetail -> {
            val gallery = kind.gallery
            val displayTitle = gallery.title?.takeIf { it.isNotBlank() }
                ?: gallery.titleJpn?.takeIf { it.isNotBlank() }
                ?: "(Untitled)"
            "$displayTitle - EhViewer"
        }
    }

    fun openOrFocusGallery(
        windows: List<ShellWindow>,
        gallery: BaseGalleryInfo,
        nextIdProvider: () -> Long,
    ): Pair<List<ShellWindow>, Long> {
        val existing = windows.firstOrNull {
            val kind = it.kind
            kind is DesktopWindowKind.GalleryDetail && kind.gallery.gid == gallery.gid
        }
        if (existing != null) {
            return windows to existing.id
        }

        val newId = nextIdProvider()
        val newWindow = ShellWindow(id = newId, kind = DesktopWindowKind.GalleryDetail(gallery))
        return (windows + newWindow) to newId
    }

    fun closeWindow(windows: List<ShellWindow>, id: Long): List<ShellWindow> = windows.filterNot { it.id == id }
}
