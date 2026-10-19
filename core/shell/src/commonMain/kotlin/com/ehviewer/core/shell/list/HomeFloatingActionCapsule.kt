package com.ehviewer.core.shell.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.ehviewer.core.i18n.MR
import com.ehviewer.core.ui.component.LiquidGlassSurface
import com.ehviewer.core.ui.icons.EhIcons
import com.ehviewer.core.ui.icons.filled.GoTo
import com.ehviewer.core.ui.icons.filled.LastPage
import com.ehviewer.core.ui.icons.filled.Shuffle
import dev.icerock.moko.resources.compose.stringResource
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 首页悬浮液态玻璃操作胶囊（移动端 HomeFloatingActionCapsule 同构移植）。
// 替代旧式多级垂直 Speed-Dial FAB：随机跳页 / 刷新 / 跳转 / 末位 一排直达；
// 可选动作传 null 即收起对应按钮。避让（snackbar/底栏）由宿主 modifier 处理。
@Composable
fun HomeFloatingActionCapsule(
    visible: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    onShuffle: (() -> Unit)? = null,
    onGoTo: (() -> Unit)? = null,
    onLastPage: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier,
    ) {
        LiquidGlassSurface(
            shape = CircleShape,
            elevation = 6.dp,
            modifier = Modifier.height(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onShuffle != null) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onShuffle()
                        },
                        minWidth = 36.dp,
                        minHeight = 36.dp,
                        cornerRadius = 18.dp,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = EhIcons.Default.Shuffle,
                            contentDescription = stringResource(MR.strings.action_shuffle),
                            tint = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRefresh()
                    },
                    minWidth = 36.dp,
                    minHeight = 36.dp,
                    cornerRadius = 18.dp,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = MiuixIcons.Refresh,
                        contentDescription = stringResource(MR.strings.refresh),
                        tint = MiuixTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }

                if (onGoTo != null) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onGoTo()
                        },
                        minWidth = 36.dp,
                        minHeight = 36.dp,
                        cornerRadius = 18.dp,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = EhIcons.Default.GoTo,
                            contentDescription = stringResource(MR.strings.action_go_to),
                            tint = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                if (onLastPage != null) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLastPage()
                        },
                        minWidth = 36.dp,
                        minHeight = 36.dp,
                        cornerRadius = 18.dp,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = EhIcons.Default.LastPage,
                            contentDescription = stringResource(MR.strings.action_last_page),
                            tint = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
