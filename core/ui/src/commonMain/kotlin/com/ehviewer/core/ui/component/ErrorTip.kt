package com.ehviewer.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ehviewer.core.ui.icons.EhIcons
import com.ehviewer.core.ui.icons.big.SadAndroid
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 空态/错误提示的唯一实现：移动端与桌面列表共用。
// retryText 由调用方按各自资源模块传入，避免 core-ui 反向依赖 i18n。
@Composable
fun ErrorTip(
    modifier: Modifier = Modifier,
    text: String,
    retryText: String? = null,
    enabled: Boolean = true,
    onRetry: (() -> Unit)? = null,
    compact: Boolean = false,
) {
    if (compact) {
        ErrorTipContent(
            modifier = modifier.padding(vertical = 12.dp),
            text = text,
            retryText = retryText,
            enabled = enabled,
            onRetry = onRetry,
            compact = true,
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            ErrorTipContent(
                modifier = modifier,
                text = text,
                retryText = retryText,
                enabled = enabled,
                onRetry = onRetry,
                compact = false,
            )
        }
    }
}

@Composable
private fun ErrorTipContent(
    modifier: Modifier,
    text: String,
    retryText: String?,
    enabled: Boolean,
    onRetry: (() -> Unit)?,
    compact: Boolean,
) {
    // 紧凑态真正收紧图标与间距，避免页脚错误提示占据接近一整屏的高度。
    val iconSize = if (compact) 24.dp else 120.dp
    val iconPadding = if (compact) 4.dp else 16.dp
    val actionSpacing = if (compact) 8.dp else 16.dp
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = EhIcons.Big.Default.SadAndroid,
            contentDescription = null,
            modifier = Modifier.padding(iconPadding).size(iconSize),
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Text(
            text = text,
            style = if (compact) MiuixTheme.textStyles.footnote1 else MiuixTheme.textStyles.body1,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null && retryText != null) {
            Spacer(modifier = Modifier.height(actionSpacing))
            Button(
                enabled = enabled,
                onClick = onRetry,
            ) {
                Text(text = retryText)
            }
        }
    }
}
