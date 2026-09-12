package com.ehviewer.core.ui.component

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.squircle.addSquircleRect

/**
 * 严格对齐 Xiaomi HyperOS / MIUIX 规范的连续曲率超椭圆 Shape。
 */
class SquircleShape(
    val cornerRadius: Dp = 12.dp,
    val squircleEnabled: Boolean = true,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val radiusPx = with(density) { cornerRadius.toPx() }
        val path = Path().apply {
            addSquircleRect(
                width = size.width,
                height = size.height,
                cornerRadius = radiusPx,
                squircleEnabled = squircleEnabled,
            )
        }
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SquircleShape) return false
        return cornerRadius == other.cornerRadius &&
            squircleEnabled == other.squircleEnabled
    }

    override fun hashCode(): Int {
        var result = cornerRadius.hashCode()
        result = 31 * result + squircleEnabled.hashCode()
        return result
    }
}
