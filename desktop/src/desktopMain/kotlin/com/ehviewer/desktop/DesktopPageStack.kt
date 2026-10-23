package com.ehviewer.desktop

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.ehviewer.core.model.BaseGalleryInfo
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import top.yukonga.miuix.kmp.nav.core.NavKey

// 单窗口页面模型：所有页面共用一个 OS 窗口，栈顶为当前页；库页是永不出栈的根。
// 纯模型无组合语境，会话快照/标题逻辑一并内聚于此（标签经参数注入保持无 i18n 依赖）。
// 栈承载迁至 miuix-nav NavBackStack（SnapshotStateList<NavKey>），本对象保留栈语义与会话逻辑。
sealed interface DesktopPage : NavKey {
    data object Library : DesktopPage
    data object Settings : DesktopPage
    data object NavItems : DesktopPage
    data class GalleryDetail(val gallery: BaseGalleryInfo) : DesktopPage
    data class Reader(val gallery: BaseGalleryInfo) : DesktopPage
}

object DesktopPageStack {
    // 会话恢复上限：防止异常状态积累导致启动时铺满页面栈
    const val MAX_RESTORE_WINDOWS = 10
    const val MAX_RESTORE_LIMIT = 20

    // 共享不可变根栈：库页永不出栈，所有"回到根"路径返回同一实例
    private val rootStack: List<DesktopPage> = listOf(DesktopPage.Library)

    fun initial(): List<DesktopPage> = rootStack

    // 栈顶同页不重复推入（双击/重复触发防抖），其余追加到栈顶
    fun pushPage(stack: SnapshotStateList<NavKey>, page: DesktopPage) {
        if (stack.lastOrNull() == page) return
        stack.add(page)
    }

    // 根保护：库页与空栈 pop 均不产生空栈
    fun popPage(stack: SnapshotStateList<NavKey>): Boolean = if (stack.size > 1) {
        stack.removeAt(stack.lastIndex)
        true
    } else {
        false
    }

    fun popToRootPage(stack: SnapshotStateList<NavKey>) {
        while (stack.size > 1 && stack.last() != DesktopPage.Library) stack.removeAt(stack.lastIndex)
    }

    // hydrate 回写：栈顶详情页的占位画廊被真实元数据整体替换；栈顶非详情页时原样不动
    fun updateTopGallery(stack: SnapshotStateList<NavKey>, gallery: BaseGalleryInfo) {
        if (stack.lastOrNull() is DesktopPage.GalleryDetail) {
            stack[stack.lastIndex] = DesktopPage.GalleryDetail(gallery)
        }
    }

    fun detailSnapshot(stack: List<NavKey>): List<SessionGallery> = stack.mapNotNull { page ->
        (page as? DesktopPage.GalleryDetail)?.let {
            SessionGallery(gid = it.gallery.gid, token = it.gallery.token, title = it.gallery.title, titleJpn = it.gallery.titleJpn)
        }
    }

    fun encodeSession(galleries: List<SessionGallery>): String = sessionJson.encodeToString(ListSerializer(SessionGallery.serializer()), galleries)

    fun decodeSession(raw: String?, limit: Int = MAX_RESTORE_WINDOWS): List<SessionGallery> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            sessionJson.decodeFromString(ListSerializer(SessionGallery.serializer()), raw)
        }.getOrDefault(emptyList()).take(limit)
    }

    // 会话恢复语义：库页在上，快照中的详情链按原顺序回填（占位信息由 hydrator 补齐）
    fun restorePages(saved: List<SessionGallery>): List<DesktopPage> = initial() + saved.map { gallery ->
        DesktopPage.GalleryDetail(
            BaseGalleryInfo(gid = gallery.gid, token = gallery.token, title = gallery.title, titleJpn = gallery.titleJpn),
        )
    }

    // 会话恢复上限设置项的合法化：非法（非正/越界）回默认 10
    fun sanitizeRestoreLimit(raw: Int): Int = if (raw in 1..MAX_RESTORE_LIMIT) raw else MAX_RESTORE_WINDOWS

    // 单窗口标题跟随栈顶页面；无标题画廊回退 untitled 标签（不再裸回退 GID）
    fun pageTitle(
        page: NavKey,
        untitledLabel: String,
        settingsLabel: String,
        readingLabel: String,
        navItemsLabel: String,
    ): String = when (val p = page) {
        DesktopPage.Library -> "EhViewer"
        DesktopPage.Settings -> "EhViewer $settingsLabel"
        DesktopPage.NavItems -> "EhViewer $navItemsLabel"
        is DesktopPage.GalleryDetail -> "${p.gallery.displayTitle(untitledLabel)} - EhViewer"
        is DesktopPage.Reader -> "${p.gallery.displayTitle(untitledLabel)} - $readingLabel"
        else -> "EhViewer"
    }

    private val sessionJson = Json { ignoreUnknownKeys = true }

    @kotlinx.serialization.Serializable
    data class SessionGallery(val gid: Long, val token: String, val title: String?, val titleJpn: String?)
}

// 无标题时的展示回退：标题 → 日文标题 → untitled 标签（界面层注入 i18n 文案）
private fun BaseGalleryInfo.displayTitle(untitledLabel: String): String = title?.takeIf { it.isNotBlank() }
    ?: titleJpn?.takeIf { it.isNotBlank() }
    ?: untitledLabel
