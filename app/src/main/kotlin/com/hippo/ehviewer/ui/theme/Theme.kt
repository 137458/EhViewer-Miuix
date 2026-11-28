package com.hippo.ehviewer.ui.theme

import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.ehviewer.core.ui.component.scrollbarStyle
import com.ehviewer.core.ui.theme.rememberMiuixThemeController
import com.ehviewer.core.util.isAtLeastOMR1
import com.ehviewer.core.util.isAtLeastS
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.collectAsState
import com.materialkolor.dynamicColorScheme
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.TextStyles

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

// Settings.theme 存的是 AppCompatDelegate night mode 值（arrays.xml night_mode_values = [-1, 1, 2]）
@PublishedApi
internal fun resolveUseDarkTheme(theme: Int, isSystemDarkTheme: Boolean) = when (theme) {
    1 -> false
    2 -> true
    else -> isSystemDarkTheme
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
            // 移动端滚动条沿用单一主色（静息与悬停同色，保持既有行为）
            val scrollbarStyle = scrollbarStyle(
                thumbColor = MiuixTheme.colorScheme.primary,
                hoverThumbColor = MiuixTheme.colorScheme.primary,
            )
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onBackground,
                LocalScrollbarStyle provides scrollbarStyle,
                content = content,
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * 让状态栏 / 导航栏图标亮度跟随应用内深浅色设置，而不是只跟随系统深色。
 * 阅读器页自己用 rememberSystemUiController 接管状态栏，退出时会恢复这里写入的值。
 */
@Composable
fun SyncSystemBarAppearance(useDarkTheme: Boolean) {
    val view = LocalView.current
    val window = LocalContext.current.findActivity()?.window ?: return
    SideEffect {
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !useDarkTheme
        controller.isAppearanceLightNavigationBars = !useDarkTheme
    }
}

@Composable
fun Color.scrim() = copy(alpha = if (MiuixTheme.colorScheme.surface.luminance() < 0.5f) 0.5f else 0.9f)

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
