package com.ehviewer.core.ui.util

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 主导航形态：窄屏使用悬浮底栏，真正的宽屏使用侧边导航栏。
 */
enum class NavigationChrome {
    BottomBar,
    Rail,
}

/**
 * 全应用统一的窗口自适应策略。
 *
 * 判定只依赖可用宽高（dp），不依赖设备类型，因此折叠屏展开/折叠、分屏与自由窗口
 * 都能复用同一套规则，也便于单元测试。
 */
object AdaptiveLayoutPolicy {
    /** 侧边导航栏要求的最小宽度。 */
    const val RAIL_MIN_WIDTH_DP = 600

    /** 侧边导航栏要求的最小高度。 */
    const val RAIL_MIN_HEIGHT_DP = 600

    /** 单栏内容开始需要限宽的最小窗口宽度。 */
    const val CONTENT_MAX_WIDTH_MIN_DP = 600

    /** 使用宽屏限宽档的最小窗口宽度。 */
    const val CONTENT_WIDE_WIDTH_MIN_DP = 840

    /** 单栏内容最大宽度（宽屏）。 */
    val CONTENT_MAX_WIDTH = 840.dp

    /** 单栏内容最大宽度（横屏手机/窄平板这类 600~839dp 窗口）。 */
    val CONTENT_MAX_WIDTH_COMPACT = 640.dp

    /** 缩略图瀑布流单列的最小宽度。 */
    const val THUMB_MIN_COLUMN_WIDTH_DP = 200

    /**
     * 侧边导航栏要求宽高同时达到 600dp。
     *
     * 只用宽度判定会把 800x412dp 这类横屏手机也当成宽屏，而 8 项侧栏在该高度下必然溢出，
     * 导致条目被裁切且无法点击。
     */
    fun navigationChrome(widthDp: Int, heightDp: Int): NavigationChrome = if (widthDp >= RAIL_MIN_WIDTH_DP && heightDp >= RAIL_MIN_HEIGHT_DP) {
        NavigationChrome.Rail
    } else {
        NavigationChrome.BottomBar
    }

    fun isLandscape(widthDp: Int, heightDp: Int): Boolean = widthDp > heightDp

    /**
     * 横屏矮视口（如 800x412dp）。
     *
     * 这类窗口高度不足 600dp，弹层必须走紧凑形态：日历/时钟选择器改用输入模式、
     * 内容加滚动兜底，否则会被窗口裁切。
     */
    fun isShortLandscape(widthDp: Int, heightDp: Int): Boolean = isLandscape(widthDp, heightDp) && heightDp < RAIL_MIN_HEIGHT_DP

    /**
     * 单栏可读内容的最大宽度；null 表示不限制。
     *
     * 600~839dp（横屏手机、竖屏平板）也要限宽，否则单栏内容会被拉伸到接近整屏宽。
     */
    fun contentMaxWidth(widthDp: Int): Dp? = when {
        widthDp >= CONTENT_WIDE_WIDTH_MIN_DP -> CONTENT_MAX_WIDTH
        widthDp >= CONTENT_MAX_WIDTH_MIN_DP -> CONTENT_MAX_WIDTH_COMPACT
        else -> null
    }

    /**
     * 缩略图瀑布流列数：宽屏下按可用宽度自适应补足列数，避免固定列数把卡片拉得过宽；
     * 结果不会小于用户配置的列数。
     */
    fun thumbGridColumns(
        widthDp: Int,
        configuredColumns: Int,
        minColumnWidthDp: Int = THUMB_MIN_COLUMN_WIDTH_DP,
    ): Int {
        val adaptive = (widthDp / minColumnWidthDp).coerceAtLeast(1)
        return maxOf(configuredColumns.coerceAtLeast(1), adaptive)
    }

    /**
     * 详情列表（GridCells.Adaptive）的最小列宽。
     *
     * 宽屏下压到可用宽度的一半，保证至少排两列；否则 480dp 的最小列宽会在 800dp 窗口里
     * 退化成单列满宽。
     */
    fun detailMinColumnWidth(availableWidthDp: Int, configuredMinWidthDp: Int): Int {
        if (availableWidthDp < CONTENT_MAX_WIDTH_MIN_DP) return configuredMinWidthDp
        return minOf(configuredMinWidthDp, (availableWidthDp / 2).coerceAtLeast(1))
    }
}

/**
 * 当前窗口的可用宽高（dp）。
 *
 * 页面据此判断横竖屏与导航形态，避免各自重复读取 Configuration 并各自定断点。
 */
@Immutable
data class WindowLayout(
    val widthDp: Int,
    val heightDp: Int,
) {
    val isLandscape: Boolean get() = AdaptiveLayoutPolicy.isLandscape(widthDp, heightDp)

    val isShortLandscape: Boolean get() = AdaptiveLayoutPolicy.isShortLandscape(widthDp, heightDp)

    val navigationChrome: NavigationChrome get() = AdaptiveLayoutPolicy.navigationChrome(widthDp, heightDp)

    val contentMaxWidth: Dp? get() = AdaptiveLayoutPolicy.contentMaxWidth(widthDp)
}

val LocalWindowLayout = staticCompositionLocalOf { WindowLayout(widthDp = 0, heightDp = 0) }

/**
 * 宽屏下把单栏内容限制在可读宽度；窄屏不生效。
 *
 * 只收窄最大宽度，居中仍由父容器的对齐方式决定，便于替换既有的硬编码 `widthIn(max = ...)`。
 */
@Composable
fun Modifier.readableWidth(): Modifier {
    val maxWidth = LocalWindowLayout.current.contentMaxWidth ?: return this
    return this.widthIn(max = maxWidth)
}
