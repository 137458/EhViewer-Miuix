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
 * 全应用统一的自适应断点。
 *
 * 窗口级判定（横竖屏、导航形态、可读宽度）一律由 [WindowLayout] 提供，本对象只保留断点常量，
 * 避免各页面各自定义阈值。
 */
object AdaptiveBreakpoints {
    /** 侧边导航栏要求的最小宽度。 */
    const val RAIL_MIN_WIDTH_DP = 600

    /** 侧边导航栏要求的最小高度。 */
    const val RAIL_MIN_HEIGHT_DP = 600

    /** 单栏内容开始需要限宽的最小窗口宽度。 */
    const val CONTENT_MAX_WIDTH_MIN_DP = 600

    /** 使用宽屏限宽档的最小窗口宽度。 */
    const val CONTENT_WIDE_WIDTH_MIN_DP = 840

    /** 弹层需要走紧凑形态的最大窗口高度。 */
    const val COMPACT_HEIGHT_MIN_DP = 500

    /** 缩略图瀑布流单列的最小宽度。 */
    const val THUMB_MIN_COLUMN_WIDTH_DP = 200

    /** 横屏矮视口下侧栏抽屉的宽度上限。 */
    const val SHEET_COMPACT_MAX_WIDTH_DP = 420

    /** 弹层高度占可用窗口高度的上限比例。 */
    const val OVERLAY_MAX_HEIGHT_RATIO = 0.6f

    /** 单栏内容最大宽度（宽屏）。 */
    val CONTENT_MAX_WIDTH = 840.dp

    /** 单栏内容最大宽度（横屏手机/窄平板这类 600~839dp 窗口）。 */
    val CONTENT_MAX_WIDTH_COMPACT = 640.dp

    /**
     * 条漫单页的最大阅读宽度。
     *
     * 条漫按视口宽度铺满，宽屏下会把画面拉到整屏宽、单屏只能看到很少的内容，
     * 因此窗口比它宽时转为左右留白并居中。窗口比它窄时该上限自然不生效。
     */
    val READING_MAX_WIDTH = 640.dp
}

/**
 * 当前窗口的可用宽高（dp）及其派生判定。
 *
 * 判定只依赖可用宽高，不依赖设备类型，因此折叠屏展开/折叠、分屏与自由窗口都能复用同一套规则。
 * 所有页面一律从这里取窗口级判定，不再各自读取 Configuration 定断点。
 */
@Immutable
data class WindowLayout(
    val widthDp: Int,
    val heightDp: Int,
) {
    val isLandscape: Boolean get() = widthDp > heightDp

    /**
     * 横屏矮视口（如 800x412dp）。
     *
     * 这类窗口高度不足 600dp，弹层必须走紧凑形态：日历/时钟选择器改用输入模式、
     * 内容加滚动兜底，否则会被窗口裁切。
     */
    val isShortLandscape: Boolean
        get() = isLandscape && heightDp < AdaptiveBreakpoints.RAIL_MIN_HEIGHT_DP

    /** 矮视口（横屏手机、分屏等）：弹层与更新日志视口需要收紧。 */
    val isCompactHeight: Boolean get() = heightDp < AdaptiveBreakpoints.COMPACT_HEIGHT_MIN_DP

    /** 宽屏：达到宽屏限宽档，可使用更宽松的内边距与多列布局。 */
    val isWide: Boolean get() = widthDp >= AdaptiveBreakpoints.CONTENT_WIDE_WIDTH_MIN_DP

    /**
     * 侧边导航栏要求宽高同时达到 600dp。
     *
     * 只用宽度判定会把 800x412dp 这类横屏手机也当成宽屏，而 8 项侧栏在该高度下必然溢出，
     * 导致条目被裁切且无法点击。
     */
    val navigationChrome: NavigationChrome
        get() = if (
            widthDp >= AdaptiveBreakpoints.RAIL_MIN_WIDTH_DP &&
            heightDp >= AdaptiveBreakpoints.RAIL_MIN_HEIGHT_DP
        ) {
            NavigationChrome.Rail
        } else {
            NavigationChrome.BottomBar
        }

    /**
     * 单栏可读内容的最大宽度；null 表示不限制。
     *
     * 600~839dp（横屏手机、竖屏平板）也要限宽，否则单栏内容会被拉伸到接近整屏宽。
     */
    val contentMaxWidth: Dp?
        get() = when {
            widthDp >= AdaptiveBreakpoints.CONTENT_WIDE_WIDTH_MIN_DP -> AdaptiveBreakpoints.CONTENT_MAX_WIDTH
            widthDp >= AdaptiveBreakpoints.CONTENT_MAX_WIDTH_MIN_DP -> AdaptiveBreakpoints.CONTENT_MAX_WIDTH_COMPACT
            else -> null
        }

    /** 单栏内容需要限宽，即窗口已达到可读宽度下限。 */
    val isWideContent: Boolean get() = contentMaxWidth != null

    /** 弹层高度上限；窗口尺寸未知时返回 null，交由弹层自行决定。 */
    val overlayMaxHeight: Dp?
        get() = if (heightDp > 0) (heightDp * AdaptiveBreakpoints.OVERLAY_MAX_HEIGHT_RATIO).dp else null

    companion object {
        /**
         * 缩略图瀑布流列数：按可用宽度自适应补足列数，避免固定列数把卡片拉得过宽；
         * 结果不会小于用户配置的列数。[availableWidthDp] 传实际可用于排布的宽度。
         */
        fun thumbGridColumns(availableWidthDp: Int, configuredColumns: Int): Int {
            val adaptive = (availableWidthDp / AdaptiveBreakpoints.THUMB_MIN_COLUMN_WIDTH_DP).coerceAtLeast(1)
            return maxOf(configuredColumns.coerceAtLeast(1), adaptive)
        }

        /**
         * 详情列表（GridCells.Adaptive）的最小列宽。
         *
         * 宽屏下压到「两列并排后仍放得下」的宽度，保证至少排两列；否则 480dp 的最小列宽会在
         * 800dp 窗口里退化成单列满宽。
         *
         * [availableWidthDp] 必须是网格自身的可用宽度（不是窗口宽度），[spacingDp] 必须是该网格
         * 的列间距：Adaptive 的列数按 `(可用宽 + 间距) / (最小列宽 + 间距)` 计算，不扣掉间距
         * 就只能排出一列。
         */
        fun detailMinColumnWidth(
            availableWidthDp: Int,
            configuredMinWidthDp: Int,
            spacingDp: Int,
        ): Int {
            if (availableWidthDp < AdaptiveBreakpoints.CONTENT_MAX_WIDTH_MIN_DP) return configuredMinWidthDp
            val twoColumnWidth = ((availableWidthDp - spacingDp) / 2).coerceAtLeast(1)
            return minOf(configuredMinWidthDp, twoColumnWidth)
        }
    }
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
