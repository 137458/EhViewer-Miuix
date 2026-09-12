package com.ehviewer.core.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import top.yukonga.miuix.kmp.basic.Slider as MiuixSlider

@Composable
fun Slider(
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    steps: Int = valueRange.last - valueRange.first - 1,
    maxTickCount: Int = defaultMaxTickCount(),
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    MiuixSlider(
        value = value.toFloat(),
        onValueChange = { onValueChange(it.fastRoundToInt()) },
        modifier = modifier,
        enabled = enabled,
        onValueChangeFinished = onValueChangeFinished,
        steps = steps.coerceAtLeast(0),
        showKeyPoints = steps in 1..<maxTickCount,
        valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
    )
}

@Composable
fun defaultMaxTickCount() = with(LocalDensity.current) {
    (LocalWindowInfo.current.containerSize.width / 40.dp.toPx()).toInt().coerceAtLeast(1)
}
