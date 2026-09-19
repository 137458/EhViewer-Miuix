package com.ehviewer.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ehviewer.core.ui.util.LocalWindowLayout
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.popup.WindowDropdownPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun DropdownFilterChip(
    label: String,
    menuItems: List<String>,
    selectedItemIndex: Int,
    onSelectedItemIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val isSelected = selectedItemIndex != 0
    val chipText = if (selectedItemIndex == 0) label else menuItems.getOrElse(selectedItemIndex) { label }

    val entry = remember(menuItems, selectedItemIndex) {
        DropdownEntry(
            items = menuItems.mapIndexed { index, item ->
                DropdownItem(
                    text = item,
                    selected = index == selectedItemIndex,
                    onClick = {
                        onSelectedItemIndexChange(index)
                    },
                )
            },
        )
    }

    val backgroundColor = if (isSelected) {
        MiuixTheme.colorScheme.primary.copy(alpha = 0.2f)
    } else {
        MiuixTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (isSelected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurface
    }

    Box(modifier = modifier) {
        LiquidGlassSurface(
            shape = CircleShape,
            containerColor = backgroundColor,
            elevation = if (isSelected) 2.dp else 1.dp,
            refractionHeight = 8.dp,
            refractionAmount = 8.dp,
            modifier = Modifier.clickable(role = Role.Button) { expanded = true },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = chipText,
                    color = contentColor,
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = MiuixIcons.Basic.ArrowRight,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(12.dp),
                )
            }
        }

        if (expanded) {
            // 横屏矮视口下限制下拉高度，否则长列表（如语言）会超出窗口被裁切
            val dropdownMaxHeight = LocalWindowLayout.current.overlayMaxHeight
            WindowDropdownPopup(
                entry = entry,
                show = expanded,
                onDismiss = { expanded = false },
                onDismissFinished = {},
                maxHeight = dropdownMaxHeight,
                dropdownColors = DropdownDefaults.dropdownColors(),
            )
        }
    }
}
