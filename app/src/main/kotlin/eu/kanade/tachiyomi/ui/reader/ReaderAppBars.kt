package eu.kanade.tachiyomi.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ehviewer.core.ui.util.AdaptiveBreakpoints
import com.ehviewer.core.ui.util.LocalWindowLayout
import com.hippo.ehviewer.ui.main.NavigationIcon
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextOverflow
import com.ehviewer.core.ui.component.LiquidGlassSurface
import com.ehviewer.core.ui.component.SquircleShape
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val animationSpec = tween<IntOffset>(200)

@Composable
context(navigator: DestinationsNavigator)
fun BoxScope.ReaderAppBars(
    visible: Boolean,
    title: String,
    isRtl: Boolean,
    showSeekBar: Boolean,
    currentPage: Int,
    totalPages: Int,
    onSliderValueChange: (Int) -> Unit,
    onClickSettings: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.systemBarsIgnoringVisibility.only(WindowInsetsSides.Horizontal))
            .align(Alignment.TopCenter),
        enter = slideInVertically(initialOffsetY = { -it }, animationSpec = animationSpec),
        exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = animationSpec),
    ) {
        val windowLayout = LocalWindowLayout.current
        val horizontalPadding = if (windowLayout.isWide) 32.dp else 16.dp
        val verticalPadding = if (windowLayout.isShortLandscape) 6.dp else 12.dp
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                .widthIn(max = AdaptiveBreakpoints.READER_BAR_MAX_WIDTH_DP.dp)
                .fillMaxWidth(),
        ) {
            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = SquircleShape(20.dp),
                elevation = 8.dp,
                containerColor = MiuixTheme.colorScheme.surfaceContainer,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NavigationIcon()
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = title,
                        style = MiuixTheme.textStyles.title4,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (totalPages > 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$currentPage/$totalPages",
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                }
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.systemBarsIgnoringVisibility.only(WindowInsetsSides.Horizontal))
            .align(Alignment.BottomCenter),
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = animationSpec),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = animationSpec),
    ) {
        val windowLayout = LocalWindowLayout.current
        // 横屏/宽屏用更宽松的左右内边距；矮视口收紧纵向间距，避免 chrome 吃掉近半屏高
        val horizontalPadding = if (windowLayout.isWide) 32.dp else 16.dp
        val verticalPadding = if (windowLayout.isShortLandscape) 6.dp else 12.dp
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                .widthIn(max = AdaptiveBreakpoints.READER_BAR_MAX_WIDTH_DP.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(if (windowLayout.isShortLandscape) 4.dp else 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (showSeekBar && totalPages > 1) {
                ChapterNavigator(
                    isRtl = isRtl,
                    currentPage = currentPage,
                    totalPages = totalPages,
                    onSliderValueChange = onSliderValueChange,
                )
            }
            BottomReaderBar(onClickSettings = onClickSettings)
        }
    }
}
