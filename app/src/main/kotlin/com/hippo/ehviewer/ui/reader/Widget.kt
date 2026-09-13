package com.hippo.ehviewer.ui.reader

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ehviewer.core.ui.component.Slider
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference

@Composable
fun SpinnerChoice(title: String, entries: Array<String>, values: List<Int>, field: MutableState<Int>) {
    var value by field
    val data = remember(entries, values) { entries zip values }
    val entry = remember(data, value) {
        DropdownEntry(
            items = data.map { (k, v) ->
                DropdownItem(
                    text = k,
                    selected = v == value,
                    onClick = { value = v },
                )
            },
        )
    }
    WindowDropdownPreference(
        title = title,
        entry = entry,
    )
}

@Composable
fun SwitchChoice(title: String, field: MutableState<Boolean>) {
    SwitchPreference(
        checked = field.value,
        onCheckedChange = { field.value = it },
        title = title,
    )
}

@Composable
fun SliderChoice(
    startSlot: @Composable () -> Unit,
    endSlot: @Composable () -> Unit,
    range: IntRange,
    field: MutableState<Int>,
) {
    var value by field
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        startSlot()
        Slider(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier.weight(1f).padding(8.dp),
            valueRange = range,
        )
        endSlot()
    }
}
