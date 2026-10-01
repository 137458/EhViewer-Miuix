package com.ehviewer.desktop

// 阅读方向：LTR 西式（右区下一页）；RTL 日漫（右区上一页，← 为下一页）。
enum class DesktopReadingDirection {
    LTR,
    RTL,
    ;

    /** 点击区域产生的页码增量：rightZone=true 为点击图片右 1/3 区域。 */
    fun pageDeltaForZone(rightZone: Boolean): Int = when {
        this == LTR && rightZone -> 1
        this == LTR && !rightZone -> -1
        this == RTL && rightZone -> -1
        else -> 1
    }

    /** 方向键产生的页码增量：forward=true 为 ←/→ 中的"下一页"方向键。 */
    fun pageDeltaForKey(forward: Boolean): Int {
        val base = if (forward) 1 else -1
        return if (this == LTR) base else -base
    }

    fun toggle(): DesktopReadingDirection = if (this == LTR) RTL else LTR

    companion object {
        fun fromPersisted(raw: String?): DesktopReadingDirection = runCatching { valueOf(raw!!) }.getOrDefault(LTR)
    }
}
