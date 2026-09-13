package com.hippo.ehviewer.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ehviewer.core.i18n.R
import com.ehviewer.core.ui.component.LiquidGlassSurface
import com.ehviewer.core.ui.icons.EhIcons
import com.ehviewer.core.ui.icons.filled.GoTo
import com.ehviewer.core.ui.icons.filled.LastPage
import com.ehviewer.core.ui.icons.filled.Shuffle
import com.ehviewer.core.ui.util.snackBarPadding
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 首页现代化悬浮液态玻璃操作胶囊 (Liquid Glass Floating Action Capsule)。
 * 替代旧式 4 级垂直 Speed-Dial FAB，提供紧凑、优雅、1-Tap 即达的快捷控制。
 */
@Composable
fun HomeFloatingActionCapsule(
    visible: Boolean,
    onRefresh: () -> Unit,
    onShuffle: (() -> Unit)? = null,
    onGoTo: (() -> Unit)? = null,
    onLastPage: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .snackBarPadding()
            .padding(end = 16.dp, bottom = 16.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            LiquidGlassSurface(
                shape = CircleShape,
                elevation = 6.dp,
                modifier = Modifier.height(48.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onShuffle != null) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onShuffle()
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = EhIcons.Default.Shuffle,
                                contentDescription = stringResource(R.string.action_shuffle),
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
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Refresh,
                            contentDescription = stringResource(R.string.refresh),
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
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = EhIcons.Default.GoTo,
                                contentDescription = stringResource(R.string.action_go_to),
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
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = EhIcons.Default.LastPage,
                                contentDescription = stringResource(R.string.action_last_page),
                                tint = MiuixTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
