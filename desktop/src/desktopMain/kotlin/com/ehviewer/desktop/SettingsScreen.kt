package com.ehviewer.desktop

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ehviewer.core.i18n.MR
import dev.icerock.moko.resources.compose.stringResource
import top.yukonga.miuix.kmp.basic.BasicComponent

// 桌面设置页：写入共享偏好层后主题等消费方即时生效
@Composable
fun SettingsScreen() {
    val themeMode by DesktopSettings.themeMode.valueFlow().collectAsState(DesktopSettings.themeMode.value)
    val themeLabel = when (themeMode) {
        1 -> stringResource(MR.strings.theme_light)
        2 -> stringResource(MR.strings.theme_dark)
        else -> stringResource(MR.strings.theme_follow_system)
    }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            BasicComponent(
                title = stringResource(MR.strings.settings_theme),
                summary = themeLabel,
                onClick = { DesktopSettings.themeMode.value = (themeMode + 1) % 3 },
            )
        }
    }
}
