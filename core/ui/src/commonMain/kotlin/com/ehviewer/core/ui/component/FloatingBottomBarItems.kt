// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
//
// Adapted from InstallerX-Revived (com.rosan.installer.ui.library.FloatingBottomBar)
// and compose-miuix-ui official example (component.liquid.LiquidGlassNavigationBar)
// with core physics and shader pipeline from Kyant0/AndroidLiquidGlass (Apache 2.0).

/*
 * [FloatingBottomBar] 的 Tab 项 DSL 组件（自 FloatingBottomBar.kt 拆分而来，纯代码搬运）。
 *
 * [FloatingBottomBarItem] 自动继承动态图层颜色（[LocalFloatingBottomBarContentColor]）与
 * 按压缩放（[LocalFloatingBottomBarTabScale]），并支持点击、键盘激活（Enter / 小键盘 Enter / 空格）
 * 与无障碍 Tab 语义。
 */

package com.ehviewer.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.LocalContentColor as MiuixLocalContentColor

/**
 * DSL Item for [FloatingBottomBar].
 * Automatically inherits dynamic layer colors and press scaling.
 */
@Composable
internal fun RowScope.FloatingBottomBarItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scale = LocalFloatingBottomBarTabScale.current
    val contentColor = LocalFloatingBottomBarContentColor.current
    val inputEnabled = LocalFloatingBottomBarInputEnabled.current
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clip(CircleShape)
            .then(
                // 录制层（alpha = 0）不参与交互：否则会和真实层叠出重复的点击/焦点目标
                if (inputEnabled) {
                    Modifier
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Tab,
                            onClick = onClick,
                        )
                        .onKeyEvent { event ->
                            val isActivationKey = event.key == Key.Enter ||
                                event.key == Key.NumPadEnter ||
                                event.key == Key.Spacebar
                            if (isActivationKey) {
                                if (event.type == KeyEventType.KeyUp) onClick()
                                true
                            } else {
                                false
                            }
                        }
                        .focusable()
                } else {
                    Modifier
                },
            )
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                val s = scale()
                scaleX = s
                scaleY = s
            },
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
        horizontalAlignment = CenterHorizontally,
    ) {
        CompositionLocalProvider(
            MiuixLocalContentColor provides contentColor,
        ) {
            content()
        }
    }
}
