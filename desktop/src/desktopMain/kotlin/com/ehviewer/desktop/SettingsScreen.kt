package com.ehviewer.desktop

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ehviewer.core.i18n.MR
import dev.icerock.moko.resources.compose.stringResource
import top.yukonga.miuix.kmp.basic.BasicComponent

// 桌面设置页：写入共享偏好层后主题、代理等消费方即时生效
@Composable
fun SettingsScreen() {
    val themeMode by DesktopSettings.themeMode.valueFlow().collectAsState(DesktopSettings.themeMode.value)
    val themeLabel = when (themeMode) {
        1 -> stringResource(MR.strings.theme_light)
        2 -> stringResource(MR.strings.theme_dark)
        else -> stringResource(MR.strings.theme_follow_system)
    }
    val proxyValue by DesktopSettings.proxy.valueFlow().collectAsState(DesktopSettings.proxy.value)
    var proxyText by remember(proxyValue) { mutableStateOf(proxyValue.orEmpty()) }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            BasicComponent(
                title = stringResource(MR.strings.settings_theme),
                summary = themeLabel,
                onClick = { DesktopSettings.themeMode.value = (themeMode + 1) % 3 },
            )
        }
        item {
            OutlinedTextField(
                value = proxyText,
                onValueChange = {
                    proxyText = it
                    DesktopSettings.proxy.value = it.trim().takeIf { v -> v.isNotEmpty() }
                },
                label = { Text(stringResource(MR.strings.settings_proxy)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }
    }
}
