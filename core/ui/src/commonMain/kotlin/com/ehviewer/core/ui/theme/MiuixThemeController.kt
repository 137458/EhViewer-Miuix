package com.ehviewer.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme

// 端侧共享的 Miuix 主题控制器：浅色为 HyperOS 低饱和基底、深色支持 AMOLED 纯黑。
// Android 端经 EhTheme 包裹（外层另叠 M3 动态取色），桌面端直接消费，保证两端同源配色。
@Composable
fun rememberMiuixThemeController(useDarkTheme: Boolean, isAmoled: Boolean): ThemeController = remember(useDarkTheme, isAmoled) {
    val mode = if (useDarkTheme) ColorSchemeMode.Dark else ColorSchemeMode.Light
    val lightColors = miuixLightColorScheme(
        background = Color(0xFFF6F7F9),
        surface = Color(0xFFF6F7F9),
        surfaceContainer = Color.White,
        surfaceContainerHigh = Color(0xFFF0F1F4),
        surfaceContainerHighest = Color(0xFFE5E7EB),
        onBackground = Color(0xFF191919),
        onSurface = Color(0xFF191919),
        onSurfaceContainer = Color(0xFF191919),
        onSurfaceVariantSummary = Color(0xFF666666),
        onSurfaceSecondary = Color(0xFF888888),
    )
    val darkColors = if (isAmoled) {
        miuixDarkColorScheme(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF121212),
            surfaceContainer = Color.Black,
            surfaceContainerHigh = Color(0xFF1E1E1E),
            surfaceContainerHighest = Color(0xFF2C2C2C),
            onBackground = Color(0xFFF3F4F6),
            onSurface = Color(0xFFF3F4F6),
            onSurfaceContainer = Color(0xFFF3F4F6),
            onSurfaceVariantSummary = Color(0xFF9CA3AF),
            onSurfaceSecondary = Color(0xFF9CA3AF),
        )
    } else {
        miuixDarkColorScheme()
    }
    ThemeController(
        colorSchemeMode = mode,
        lightColors = lightColors,
        darkColors = darkColors,
    )
}
