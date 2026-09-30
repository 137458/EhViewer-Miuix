package com.ehviewer.core.ui.util

import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.ui.Modifier

actual fun Modifier.excludeSystemGesture(): Modifier = systemGestureExclusion()
