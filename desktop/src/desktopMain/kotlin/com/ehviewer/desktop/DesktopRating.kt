package com.ehviewer.desktop

import kotlin.math.roundToInt

object DesktopRating {
    fun formatRatingScore(rating: Float): String {
        if (rating <= 0.0f) return "0.00"
        if (rating >= 5.0f) return "5.00"
        val scaled = (rating * 100).roundToInt()
        val clamped = scaled.coerceIn(0, 500)
        val integerPart = clamped / 100
        val fractionalPart = clamped % 100
        return "$integerPart.${if (fractionalPart < 10) "0$fractionalPart" else "$fractionalPart"}"
    }

    // 类目显示名由调用方经 DesktopCategories 注入
    fun formatCardMeta(pages: Int, categoryName: String): String {
        return if (pages > 0) {
            "${pages}P · $categoryName"
        } else {
            categoryName
        }
    }
}
