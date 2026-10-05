package com.ehviewer.desktop

import com.ehviewer.core.model.BaseGalleryInfo
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

// 单窗口页面模型：所有页面共用一个 OS 窗口，栈顶为当前页；库页是永不出栈的根。
// 纯模型无组合语境，会话快照/标题逻辑一并内聚于此（标签经参数注入保持无 i18n 依赖）。
sealed interface DesktopPage {
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
    fun push(stack: List<DesktopPage>, page: DesktopPage): List<DesktopPage> = if (stack.lastOrNull() == page) stack else stack + page

    // 根保护：库页与空栈 pop 均不产生空栈
    fun pop(stack: List<DesktopPage>): List<DesktopPage> = when {
        stack.isEmpty() -> rootStack
        stack.size == 1 -> stack
        else -> stack.dropLast(1)
    }

    fun popToRoot(stack: List<DesktopPage>): List<DesktopPage> = rootStack

    // hydrate 回写：栈顶详情页的占位画廊被真实元数据整体替换；栈顶非详情页时原样返回
    fun updateTopGallery(stack: List<DesktopPage>, gallery: BaseGalleryInfo): List<DesktopPage> = if (stack.lastOrNull() is DesktopPage.GalleryDetail) {
        stack.dropLast(1) + DesktopPage.GalleryDetail(gallery)
    } else {
        stack
    }

    fun detailSnapshot(stack: List<DesktopPage>): List<SessionGallery> = stack.mapNotNull { page ->
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
        page: DesktopPage,
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
    }

    private val sessionJson = Json { ignoreUnknownKeys = true }

    @kotlinx.serialization.Serializable
    data class SessionGallery(val gid: Long, val token: String, val title: String?, val titleJpn: String?)
}

// 无标题时的展示回退：标题 → 日文标题 → untitled 标签（界面层注入 i18n 文案）
private fun BaseGalleryInfo.displayTitle(untitledLabel: String): String = title?.takeIf { it.isNotBlank() }
    ?: titleJpn?.takeIf { it.isNotBlank() }
    ?: untitledLabel
