package com.ehviewer.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ehviewer.core.i18n.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.FlowPreview
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 桌面设置页：写入共享偏好层后主题、代理等消费方即时生效
@OptIn(FlowPreview::class)
@Composable
fun SettingsScreen() {
    val themeMode by DesktopSettings.themeMode.valueFlow().collectAsState(DesktopSettings.themeMode.value)
    val themeLabel = when (themeMode) {
        1 -> stringResource(MR.strings.theme_light)
        2 -> stringResource(MR.strings.theme_dark)
        else -> stringResource(MR.strings.theme_follow_system)
    }
    val closeToTray by DesktopSettings.closeToTray.valueFlow().collectAsState(DesktopSettings.closeToTray.value)
    val blackDarkTheme by DesktopSettings.blackDarkTheme.valueFlow().collectAsState(DesktopSettings.blackDarkTheme.value)

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            BasicComponent(
                title = stringResource(MR.strings.settings_theme),
                summary = themeLabel,
                onClick = { DesktopSettings.themeMode.value = (themeMode + 1) % 3 },
            )
        }
        item {
            BasicComponent(
                title = stringResource(MR.strings.black_dark_theme),
                summary = if (blackDarkTheme) "✓" else "—",
                onClick = { DesktopSettings.blackDarkTheme.value = !blackDarkTheme },
            )
        }
        item {
            BasicComponent(
                title = stringResource(MR.strings.settings_close_behavior),
                summary = if (closeToTray) {
                    stringResource(MR.strings.settings_close_minimize_to_tray)
                } else {
                    stringResource(MR.strings.settings_close_exit)
                },
                onClick = { DesktopSettings.closeToTray.value = !closeToTray },
            )
        }
        item {
            val restoreSession by DesktopSettings.restoreSession.valueFlow()
                .collectAsState(DesktopSettings.restoreSession.value)
            BasicComponent(
                title = stringResource(MR.strings.settings_restore_session),
                summary = if (restoreSession) "✓" else "—",
                onClick = { DesktopSettings.restoreSession.value = !restoreSession },
            )
        }
        item {
            // 会话恢复上限：5 → 10 → 20 循环切换（经 sanitizeRestoreLimit 合法化）
            val restoreLimit by DesktopSettings.restoreLimit.valueFlow()
                .collectAsState(DesktopSettings.restoreLimit.value)
            BasicComponent(
                title = stringResource(MR.strings.settings_restore_session),
                summary = "${DesktopPageStack.sanitizeRestoreLimit(restoreLimit)}",
                onClick = {
                    val current = DesktopPageStack.sanitizeRestoreLimit(restoreLimit)
                    DesktopSettings.restoreLimit.value = when (current) {
                        5 -> 10
                        10 -> 20
                        else -> 5
                    }
                },
            )
        }
        item {
            val direction by DesktopSettings.readingDirection.valueFlow()
                .collectAsState(DesktopSettings.readingDirection.value)
            BasicComponent(
                title = stringResource(MR.strings.settings_reading_direction),
                summary = if (direction == "RTL") {
                    stringResource(MR.strings.settings_reading_direction_rtl)
                } else {
                    stringResource(MR.strings.settings_reading_direction_ltr)
                },
                onClick = {
                    DesktopSettings.readingDirection.value = if (direction == "RTL") "LTR" else "RTL"
                },
            )
        }
        item {
            ProxySettingField()
        }
        item {
            ImageSaveDirField()
        }
    }
}

// 代理输入：非空但无法解析为 host:port 时给出可见反馈（运行时会静默直连）
@OptIn(FlowPreview::class)
@Composable
private fun ProxySettingField() {
    val proxyState = rememberTextFieldState(DesktopSettings.proxy.value.orEmpty())
    LaunchedEffect(Unit) {
        snapshotFlow { proxyState.text }
            .collect { text ->
                DesktopSettings.proxy.value = text.toString().trim().takeIf { v -> v.isNotEmpty() }
            }
    }
    val proxyInvalid = remember(proxyState.text) {
        proxyState.text.isNotBlank() && parseHostPort(proxyState.text.toString().trim()) == null
    }
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        TextField(
            state = proxyState,
            label = stringResource(MR.strings.settings_proxy),
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        if (proxyInvalid) {
            Text(
                text = stringResource(MR.strings.settings_proxy_invalid),
                color = Color.Red.copy(alpha = 0.8f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

// 图片保存目录：空 = 默认下载目录 ~/Downloads/EhViewer
@OptIn(FlowPreview::class)
@Composable
private fun ImageSaveDirField() {
    val saveDirState = rememberTextFieldState(DesktopSettings.imageSaveDir.value.orEmpty())
    LaunchedEffect(Unit) {
        snapshotFlow { saveDirState.text }
            .collect { text ->
                DesktopSettings.imageSaveDir.value = text.toString().trim().takeIf { v -> v.isNotEmpty() }
            }
    }
    TextField(
        state = saveDirState,
        label = stringResource(MR.strings.settings_download_download_location),
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    )
}
