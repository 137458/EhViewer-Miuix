package com.ehviewer.core.ui.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class ContentInsetsTest {

    @Test
    fun calculateBarBottomPaddingUses8DpPlusNavBarWhenPresent() {
        val navBarPadding = 16.dp
        val barBottomPadding = BottomBarInsetsCalculator.calculateBarBottomPadding(navBarPadding)
        assertEquals(24.dp, barBottomPadding)
    }

    @Test
    fun calculateBarBottomPaddingUses20DpWhenNavBarIsZero() {
        val navBarPadding = 0.dp
        val barBottomPadding = BottomBarInsetsCalculator.calculateBarBottomPadding(navBarPadding)
        assertEquals(20.dp, barBottomPadding)
    }

    @Test
    fun calculateMainContentBottomPaddingOnPrimaryMobileScreen() {
        // Standard gesture bar (16.dp navBar): 64.dp + (8.dp + 16.dp) + 16.dp = 104.dp
        val paddingWithGesture = BottomBarInsetsCalculator.calculateMainContentBottomPadding(
            isPrimaryDestination = true,
            navigationChrome = NavigationChrome.BottomBar,
            navBarBottomPadding = 16.dp,
        )
        assertEquals(104.dp, paddingWithGesture)

        // Hidden nav bar (0.dp navBar): 64.dp + 20.dp + 16.dp = 100.dp (matching pixez spec)
        val paddingZeroNav = BottomBarInsetsCalculator.calculateMainContentBottomPadding(
            isPrimaryDestination = true,
            navigationChrome = NavigationChrome.BottomBar,
            navBarBottomPadding = 0.dp,
        )
        assertEquals(100.dp, paddingZeroNav)

        // 3-button nav bar (48.dp navBar): 64.dp + (8.dp + 48.dp) + 16.dp = 136.dp
        val paddingThreeButton = BottomBarInsetsCalculator.calculateMainContentBottomPadding(
            isPrimaryDestination = true,
            navigationChrome = NavigationChrome.BottomBar,
            navBarBottomPadding = 48.dp,
        )
        assertEquals(136.dp, paddingThreeButton)
    }

    @Test
    fun calculateMainContentBottomPaddingOnWideScreenOrSecondaryScreen() {
        val paddingWideScreen = BottomBarInsetsCalculator.calculateMainContentBottomPadding(
            isPrimaryDestination = true,
            navigationChrome = NavigationChrome.Rail,
            navBarBottomPadding = 16.dp,
        )
        assertEquals(32.dp, paddingWideScreen)

        val paddingSecondary = BottomBarInsetsCalculator.calculateMainContentBottomPadding(
            isPrimaryDestination = false,
            navigationChrome = NavigationChrome.BottomBar,
            navBarBottomPadding = 20.dp,
        )
        assertEquals(36.dp, paddingSecondary)
    }
}
