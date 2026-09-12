package com.ehviewer.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun FilledTertiaryIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = MiuixTheme.colorScheme.surfaceContainerHigh,
    cornerRadius: Dp = 12.dp,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        backgroundColor = backgroundColor,
        cornerRadius = cornerRadius,
        content = content,
    )
}

@Composable
fun FilledTertiaryIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedBackgroundColor: Color = MiuixTheme.colorScheme.primaryContainer,
    uncheckedBackgroundColor: Color = MiuixTheme.colorScheme.surfaceContainerHigh,
    cornerRadius: Dp = 12.dp,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = { onCheckedChange(!checked) },
        modifier = modifier,
        enabled = enabled,
        backgroundColor = if (checked) checkedBackgroundColor else uncheckedBackgroundColor,
        cornerRadius = cornerRadius,
        content = content,
    )
}
