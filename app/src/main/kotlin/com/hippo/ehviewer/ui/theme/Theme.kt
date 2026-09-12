package com.hippo.ehviewer.ui.theme

import android.app.WallpaperManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.scrollbar.LocalScrollbarStyle
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.ehviewer.core.ui.component.scrollbarStyle
import com.ehviewer.core.util.isAtLeastOMR1
import com.ehviewer.core.util.isAtLeastS
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.collectAsState
import com.materialkolor.dynamicColorScheme
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.TextStyles
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme

val TextStyles.titleLarge get() = title3
val TextStyles.bodyMedium get() = body2

fun ColorScheme.amoled(amoled: Boolean) = if (amoled) {
    copy(
        surface = Color.Black,
        onSurface = Color.White,
        background = Color.Black,
        onBackground = Color.White,
    )
} else {
    this
}

@Composable
fun rememberMiuixThemeController(useDarkTheme: Boolean, isAmoled: Boolean): ThemeController {
    return remember(useDarkTheme, isAmoled) {
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
}

@Composable
fun EhTheme(useDarkTheme: Boolean, content: @Composable () -> Unit) {
    val amoled by Settings.blackDarkTheme.collectAsState()
    val context = LocalContext.current
    val colors = if (isAtLeastS) {
        if (useDarkTheme) {
            dynamicDarkColorScheme(context).amoled(amoled)
        } else {
            dynamicLightColorScheme(context)
        }
    } else {
        val color = if (isAtLeastOMR1) extractWallPaperPalette() else null
        if (color != null) {
            dynamicColorScheme(
                primary = color.first,
                isDark = useDarkTheme,
                isAmoled = amoled,
                secondary = color.second,
                tertiary = color.third,
            )
        } else {
            if (useDarkTheme) {
                darkColorScheme().amoled(amoled)
            } else {
                expressiveLightColorScheme()
            }
        }
    }

    val miuixController = rememberMiuixThemeController(useDarkTheme, amoled)

    MiuixTheme(controller = miuixController) {
        MaterialTheme(colorScheme = colors, motionScheme = CustomMotionScheme) {
            val scrollbarStyle = scrollbarStyle(color = MaterialTheme.colorScheme.primary)
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onBackground,
                LocalScrollbarStyle provides scrollbarStyle,
                content = content,
            )
        }
    }
}

@Composable
fun Color.scrim() = copy(alpha = if (isSystemInDarkTheme()) 0.5f else 0.9f)

typealias WallPaperPalette = Triple<Color, Color?, Color?>

@Composable
@RequiresApi(Build.VERSION_CODES.O_MR1)
fun extractWallPaperPalette(): WallPaperPalette? {
    val colors = WallpaperManager.getInstance(LocalContext.current)?.getWallpaperColors(WallpaperManager.FLAG_SYSTEM) ?: return null
    val primary = colors.primaryColor.toArgb().let { Color(it) }
    val secondary = colors.secondaryColor?.toArgb()?.let { Color(it) }
    val tertiary = colors.tertiaryColor?.toArgb()?.let { Color(it) }
    return WallPaperPalette(primary, secondary, tertiary)
}

// https://issuetracker.google.com/363892346
object CustomMotionScheme : MotionScheme by MotionScheme.expressive() {
    override fun <T> defaultSpatialSpec() = defaultEffectsSpec<T>()
}
