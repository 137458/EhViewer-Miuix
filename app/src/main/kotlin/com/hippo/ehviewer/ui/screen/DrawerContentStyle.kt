package com.hippo.ehviewer.ui.screen

import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun selectedListItemColor() = ListItemDefaults.colors(
    containerColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.12f),
    headlineColor = MiuixTheme.colorScheme.primary,
    trailingIconColor = MiuixTheme.colorScheme.primary,
)

@Composable
fun listItemOnDrawerColor(selected: Boolean) = if (selected) {
    selectedListItemColor()
} else {
    ListItemDefaults.colors(
        containerColor = MiuixTheme.colorScheme.surface,
        headlineColor = MiuixTheme.colorScheme.onSurface,
        trailingIconColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    )
}

@Composable
fun topBarOnDrawerColor() = TopAppBarDefaults.topAppBarColors(
    containerColor = MiuixTheme.colorScheme.surface,
    scrolledContainerColor = MiuixTheme.colorScheme.surfaceContainer,
)
