package com.ehviewer.core.ui.util

import androidx.compose.runtime.Composable

@Composable
actual fun rememberHapticFeedback(): HapticFeedback = DesktopHapticFeedback

private object DesktopHapticFeedback : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}
