package com.hippo.ehviewer.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.ehviewer.core.ui.component.RollingNumber
import top.yukonga.miuix.kmp.basic.Text
import com.ehviewer.core.ui.component.SquircleShape
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun FundsItem(
    type: String,
    amount: Int,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MiuixTheme.textStyles.body2,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(SquircleShape(4.dp))
                .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.15f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = type,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.primary,
            )
        }
        RollingNumber(number = amount, style = textStyle, separator = true)
        if (type == "GP") {
            Text(text = "k", style = textStyle)
        }
    }
}
