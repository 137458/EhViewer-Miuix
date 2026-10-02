package com.ehviewer.desktop

import com.ehviewer.core.model.BaseGalleryInfo
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

sealed interface DesktopWindowKind {
    data object Library : DesktopWindowKind
    data object Settings : DesktopWindowKind
    data class GalleryDetail(val gallery: BaseGalleryInfo) : DesktopWindowKind
    data class Reader(val gallery: BaseGalleryInfo) : DesktopWindowKind
}

data class ShellWindow(val id: Long, val kind: DesktopWindowKind = DesktopWindowKind.Library) {
    constructor(id: Long, isSettings: Boolean) : this(
        id = id,
        kind = if (isSettings) DesktopWindowKind.Settings else DesktopWindowKind.Library,
    )

    val isSettings: Boolean get() = kind is DesktopWindowKind.Settings
}

object DesktopWindowManager {
    // 会话恢复上限：防止异常状态积累导致启动时铺满桌面
    const val MAX_RESTORE_WINDOWS = 10
    const val MAX_RESTORE_LIMIT = 20

    // 会话恢复上限设置项的合法化：非法（非正/越界）回默认 10
    fun sanitizeRestoreLimit(raw: Int): Int = if (raw in 1..MAX_RESTORE_LIMIT) raw else MAX_RESTORE_WINDOWS

    @kotlinx.serialization.Serializable
    data class SessionGallery(
        val gid: Long,
        val token: String,
        val title: String?,
        val titleJpn: String?,
    )

    fun sessionSnapshot(windows: List<ShellWindow>): List<SessionGallery> = windows.mapNotNull { window ->
        (window.kind as? DesktopWindowKind.GalleryDetail)?.let { kind ->
            SessionGallery(
                gid = kind.gallery.gid,
                token = kind.gallery.token,
                title = kind.gallery.title,
                titleJpn = kind.gallery.titleJpn,
            )
        }
    }

    fun encodeSession(galleries: List<SessionGallery>): String = sessionJson.encodeToString(ListSerializer(SessionGallery.serializer()), galleries)

    fun decodeSession(raw: String?, limit: Int = MAX_RESTORE_WINDOWS): List<SessionGallery> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            sessionJson.decodeFromString(ListSerializer(SessionGallery.serializer()), raw)
        }.getOrDefault(emptyList()).take(limit)
    }

    fun restoreWindows(saved: List<SessionGallery>, nextIdProvider: () -> Long): List<ShellWindow> = saved.map { gallery ->
        ShellWindow(
            id = nextIdProvider(),
            kind = DesktopWindowKind.GalleryDetail(
                BaseGalleryInfo(gid = gallery.gid, token = gallery.token, title = gallery.title, titleJpn = gallery.titleJpn),
            ),
        )
    }

    private val sessionJson = Json { ignoreUnknownKeys = true }

    fun windowTitle(
        kind: DesktopWindowKind,
        untitledLabel: String = "(Untitled)",
    ): String = when (kind) {
        DesktopWindowKind.Library -> "EhViewer"
        DesktopWindowKind.Settings -> "EhViewer Settings"
        is DesktopWindowKind.GalleryDetail -> {
            val gallery = kind.gallery
            val displayTitle = gallery.title?.takeIf { it.isNotBlank() }
                ?: gallery.titleJpn?.takeIf { it.isNotBlank() }
                ?: untitledLabel
            "$displayTitle - EhViewer"
        }
        is DesktopWindowKind.Reader -> {
            val gallery = kind.gallery
            val displayTitle = gallery.title?.takeIf { it.isNotBlank() }
                ?: gallery.titleJpn?.takeIf { it.isNotBlank() }
                ?: gallery.gid.toString()
            "$displayTitle - Reading"
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

    // hydrate 等场景的窗口内画廊信息回写：同 id 替换 GalleryDetail 保留窗口身份，非 GalleryDetail 或 id 不存在时原样返回
    fun updateGalleryWindow(
        windows: List<ShellWindow>,
        windowId: Long,
        gallery: BaseGalleryInfo,
    ): List<ShellWindow> = windows.map { window ->
        if (window.id == windowId && window.kind is DesktopWindowKind.GalleryDetail) {
            window.copy(kind = DesktopWindowKind.GalleryDetail(gallery))
        } else {
            window
        }
    }

    // 阅读窗口调度：同 gid 的 Reader 窗口防重（GalleryDetail 窗口不参与 Reader 去重）
    fun openOrFocusReader(
        windows: List<ShellWindow>,
        gallery: BaseGalleryInfo,
        nextIdProvider: () -> Long,
    ): Pair<List<ShellWindow>, Long> {
        val existing = windows.firstOrNull {
            val kind = it.kind
            kind is DesktopWindowKind.Reader && kind.gallery.gid == gallery.gid
        }
        if (existing != null) {
            return windows to existing.id
        }

        val newId = nextIdProvider()
        val newWindow = ShellWindow(id = newId, kind = DesktopWindowKind.Reader(gallery))
        return (windows + newWindow) to newId
    }

    fun closeWindow(windows: List<ShellWindow>, id: Long): List<ShellWindow> = windows.filterNot { it.id == id }
}
