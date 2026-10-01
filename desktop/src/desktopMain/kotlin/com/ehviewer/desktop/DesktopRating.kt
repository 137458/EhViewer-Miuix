package com.ehviewer.desktop

import com.ehviewer.core.database.client.getCategoryDisplayName
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

    fun formatCardMeta(pages: Int, category: Int): String {
        val catName = getCategoryDisplayName(category)
        return if (pages > 0) {
            "${pages}P · $catName"
        } else {
            catName
        }
    }
}
