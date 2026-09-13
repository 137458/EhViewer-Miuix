package com.ehviewer.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 统一的滑动删除背景，提供右对齐红色删除图标。
 *
 * 用于各 [SwipeToDismissBox] 的 backgroundContent，消除重复样板代码。
 */
@Composable
fun DismissDeleteBackground(
    modifier: Modifier = Modifier,
    endPadding: Dp = 20.dp,
    contentDescription: String? = null,
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = MiuixIcons.Delete,
            contentDescription = contentDescription,
            tint = MiuixTheme.colorScheme.error,
            modifier = Modifier.padding(end = endPadding),
        )
    }
}

/**
 * 为滑动删除列表项添加 TalkBack 自定义无障碍操作，
 * 避免读屏或运动障碍用户无法使用滑动手势触发删除。
 */
fun Modifier.dismissDeleteAction(
    label: String,
    onDelete: () -> Unit,
): Modifier = semantics {
    customActions = listOf(
        CustomAccessibilityAction(label = label) {
            onDelete()
            true
        },
    )
}
