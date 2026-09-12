package com.ehviewer.core.ui.component

import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun LabeledCheckbox(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    indication: Indication? = LocalIndication.current,
) {
    Row(
        modifier = modifier
            .clip(SquircleShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = indication,
            ) { onCheckedChange(!checked) }
            .padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(state = ToggleableState(checked), onClick = { onCheckedChange(!checked) })
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}
