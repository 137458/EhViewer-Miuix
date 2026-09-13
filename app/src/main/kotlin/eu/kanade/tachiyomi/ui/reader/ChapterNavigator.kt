package eu.kanade.tachiyomi.ui.reader

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ehviewer.core.ui.component.LiquidGlassSurface
import com.ehviewer.core.ui.component.Slider
import com.ehviewer.core.ui.component.defaultMaxTickCount
import com.ehviewer.core.ui.util.HapticFeedbackType
import com.ehviewer.core.ui.util.rememberHapticFeedback
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ChapterNavigator(
    isRtl: Boolean,
    currentPage: Int,
    totalPages: Int,
    onSliderValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MiuixTheme.colorScheme.surfaceContainer,
) = CompositionLocalProvider(LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
        elevation = 6.dp,
        containerColor = containerColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "$currentPage", style = MiuixTheme.textStyles.footnote1)
            val steps = totalPages - 2
            val maxTickCount = defaultMaxTickCount()
            val interactionSource = remember { MutableInteractionSource() }
            if (steps < maxTickCount) {
                val sliderDragged by interactionSource.collectIsDraggedAsState()
                val hapticFeedback = rememberHapticFeedback()
                LaunchedEffect(currentPage) {
                    if (sliderDragged) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.MOVE)
                    }
                }
            }
            Slider(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                value = currentPage,
                valueRange = 1..totalPages,
                steps = steps,
                onValueChange = onSliderValueChange,
                maxTickCount = maxTickCount,
                interactionSource = interactionSource,
            )
            Text(text = "$totalPages", style = MiuixTheme.textStyles.footnote1)
        }
    }
}
