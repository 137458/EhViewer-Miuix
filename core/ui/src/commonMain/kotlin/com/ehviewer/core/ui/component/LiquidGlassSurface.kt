package com.ehviewer.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ehviewer.core.ui.liquid.lens
import com.ehviewer.core.ui.liquid.vibrancy
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 通用液态玻璃材质容器 (Liquid Glass Surface)。
 * 结合超椭圆曲率、物理透镜折射、高光边缘渲染与多级优雅降级。
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = SquircleShape(24.dp),
    backdrop: Backdrop? = LocalBackdrop.current,
    containerColor: Color = MiuixTheme.colorScheme.surfaceContainer,
    elevation: Dp = 8.dp,
    refractionHeight: Dp = 16.dp,
    refractionAmount: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val isDark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    val shaderSupported = isRuntimeShaderSupported() && backdrop != null

    val specularHighlight = remember {
        Highlight(
            width = 1.dp,
            alpha = 0.8f,
            style = BloomStroke(
                color = Color.White.copy(alpha = 0.15f),
                innerBlurRadius = 2.dp,
                primaryLight = LightSource(
                    position = LightPosition(0.5f, -0.3f, -0.05f),
                    color = Color.White,
                    intensity = 1f,
                ),
                secondaryLight = LightSource(
                    position = LightPosition(0.5f, 0.8f, -0.5f),
                    color = Color.White,
                    intensity = 0.3f,
                ),
                dualPeak = true,
            ),
        )
    }

    val finalContainerColor = if (shaderSupported) {
        containerColor.copy(alpha = if (isDark) 0.5f else 0.65f)
    } else if (backdrop != null) {
        containerColor.copy(alpha = if (isDark) 0.7f else 0.85f)
    } else {
        containerColor
    }

    Box(
        modifier = modifier
            .then(
                if (elevation > 0.dp) {
                    Modifier.dropShadow(
                        shape = shape,
                        shadow = Shadow(
                            radius = elevation,
                            color = Color.Black,
                            alpha = if (isDark) 0.25f else 0.1f,
                        ),
                    )
                } else Modifier,
            )
            .clip(shape)
            .then(
                if (backdrop != null && shaderSupported) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { shape },
                        effects = {
                            padding = maxOf(padding, 32.dp.toPx())
                            vibrancy()
                            blur(4.dp.toPx(), 4.dp.toPx())
                            lens(
                                refractionHeight = refractionHeight.toPx(),
                                refractionAmount = refractionAmount.toPx(),
                            )
                        },
                        highlight = { specularHighlight },
                        onDrawSurface = { drawRect(finalContainerColor) },
                    )
                } else if (backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { shape },
                        effects = {
                            blur(25.dp.toPx(), 25.dp.toPx())
                        },
                        onDrawSurface = { drawRect(finalContainerColor) },
                    )
                } else {
                    Modifier
                        .background(finalContainerColor, shape)
                        .border(
                            width = 1.dp,
                            color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f),
                            shape = shape,
                        )
                },
            ),
        content = content,
    )
}
