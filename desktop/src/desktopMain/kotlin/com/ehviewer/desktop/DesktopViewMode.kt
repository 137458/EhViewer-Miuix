package com.ehviewer.desktop

enum class DesktopViewMode {
    List,
    Grid,
    ;

    fun toggle(): DesktopViewMode = when (this) {
        List -> Grid
        Grid -> List
    }

    companion object {
        fun fromOrdinal(ordinal: Int): DesktopViewMode = when (ordinal) {
            1 -> Grid
            else -> List
        }
    }
}
