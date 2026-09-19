package com.ehviewer.core.ui.util

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Extra bottom space reserved for the main navigation chrome.
 * Secondary routes keep a small breathing room instead of inheriting the main bar height.
 */
val LocalBottomBarContentPadding = compositionLocalOf { 16.dp }

object BottomBarInsetsCalculator {
    fun calculateBarBottomPadding(navBarBottomPadding: Dp): Dp = if (navBarBottomPadding != 0.dp) 8.dp + navBarBottomPadding else 20.dp

    fun calculateMainContentBottomPadding(
        isPrimaryDestination: Boolean,
        navigationChrome: NavigationChrome,
        navBarBottomPadding: Dp,
    ): Dp = if (isPrimaryDestination && navigationChrome == NavigationChrome.BottomBar) {
        val bottomPaddingValue = calculateBarBottomPadding(navBarBottomPadding)
        64.dp + bottomPaddingValue + 16.dp
    } else {
        16.dp + navBarBottomPadding
    }
}
