package com.ehviewer.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ehviewer.core.i18n.MR
import com.ehviewer.core.ui.component.SquircleShape
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.FlowPreview
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 桌面设置页：Miuix 卡片化层级结构，写入共享偏好层后即时生效
@OptIn(FlowPreview::class)
@Composable
fun SettingsScreen(
    onBack: (() -> Unit)? = null,
    onShowShortcuts: (() -> Unit)? = null,
    onShowAbout: (() -> Unit)? = null,
) {
    val themeMode by DesktopSettings.themeMode.valueFlow().collectAsState(DesktopSettings.themeMode.value)
    val themeLabel = when (themeMode) {
        1 -> stringResource(MR.strings.theme_light)
        2 -> stringResource(MR.strings.theme_dark)
        else -> stringResource(MR.strings.theme_follow_system)
    }
    val closeToTray by DesktopSettings.closeToTray.valueFlow().collectAsState(DesktopSettings.closeToTray.value)
    val blackDarkTheme by DesktopSettings.blackDarkTheme.valueFlow().collectAsState(DesktopSettings.blackDarkTheme.value)
    val restoreSession by DesktopSettings.restoreSession.valueFlow().collectAsState(DesktopSettings.restoreSession.value)
    val restoreLimit by DesktopSettings.restoreLimit.valueFlow().collectAsState(DesktopSettings.restoreLimit.value)
    val direction by DesktopSettings.readingDirection.valueFlow().collectAsState(DesktopSettings.readingDirection.value)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
        ) {
            // 顶部导航栏：与移动端一致的返回键 + 标题
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }
                Text(
                    text = stringResource(MR.strings.menu_settings),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = if (onBack != null) 12.dp else 4.dp),
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
            ) {
                // 外观与窗口行为
                item {
                    SmallTitle(
                        text = stringResource(MR.strings.settings_theme),
                        modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 4.dp),
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        BasicComponent(
                            title = stringResource(MR.strings.settings_theme),
                            summary = themeLabel,
                            onClick = { DesktopSettings.themeMode.value = (themeMode + 1) % 3 },
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                        )
                        SwitchPreference(
                            title = stringResource(MR.strings.black_dark_theme),
                            checked = blackDarkTheme,
                            onCheckedChange = { DesktopSettings.blackDarkTheme.value = it },
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                        )
                        BasicComponent(
                            title = stringResource(MR.strings.settings_close_behavior),
                            summary = if (closeToTray) {
                                stringResource(MR.strings.settings_close_minimize_to_tray)
                            } else {
                                stringResource(MR.strings.settings_close_exit)
                            },
                            onClick = { DesktopSettings.closeToTray.value = !closeToTray },
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                        )
                        SwitchPreference(
                            title = stringResource(MR.strings.settings_restore_session),
                            checked = restoreSession,
                            onCheckedChange = { DesktopSettings.restoreSession.value = it },
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                        )
                        if (restoreSession) {
                            BasicComponent(
                                title = stringResource(MR.strings.desktop_restore_limit),
                                summary = "${DesktopPageStack.sanitizeRestoreLimit(restoreLimit)} (5 / 10 / 20)",
                                onClick = {
                                    val current = DesktopPageStack.sanitizeRestoreLimit(restoreLimit)
                                    DesktopSettings.restoreLimit.value = when (current) {
                                        5 -> 10
                                        10 -> 20
                                        else -> 5
                                    }
                                },
                                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                            )
                        }
                    }
                }

                // 阅读偏好
                item {
                    SmallTitle(
                        text = stringResource(MR.strings.settings_reading_direction),
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp),
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
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
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                        )
                    }
                }

                // 网络与存储
                item {
                    SmallTitle(
                        text = stringResource(MR.strings.settings_advanced),
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp),
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        ProxySettingField()
                        ImageSaveDirField()
                    }
                }

                // 帮助与关于
                if (onShowShortcuts != null || onShowAbout != null) {
                    item {
                        SmallTitle(
                            text = stringResource(MR.strings.settings_about),
                            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp),
                        )
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        ) {
                            if (onShowShortcuts != null) {
                                ArrowPreference(
                                    title = stringResource(MR.strings.menu_keyboard_shortcuts),
                                    summary = "F1",
                                    onClick = onShowShortcuts,
                                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                                )
                            }
                            if (onShowAbout != null) {
                                ArrowPreference(
                                    title = stringResource(MR.strings.settings_about),
                                    summary = stringResource(MR.strings.desktop_about_version, DESKTOP_VERSION),
                                    onClick = onShowAbout,
                                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                                )
                            }
                        }
                    }
                }
            }
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                state = proxyState,
                label = stringResource(MR.strings.settings_proxy),
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.weight(1f),
            )
            if (proxyState.text.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(SquircleShape(6.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable {
                            proxyState.setTextAndPlaceCursorAtEnd("")
                            DesktopSettings.proxy.value = null
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "✕",
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = 11.sp,
                    )
                }
            }
        }
        if (proxyInvalid) {
            Text(
                text = stringResource(MR.strings.settings_proxy_invalid),
                color = MiuixTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp),
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
    val defaultDir = remember { DesktopImageSaver.resolveSaveDir(null, System.getProperty("user.home")).toString() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                state = saveDirState,
                label = stringResource(MR.strings.settings_download_download_location),
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .clip(SquircleShape(6.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable {
                        val chooser = javax.swing.JFileChooser().apply {
                            fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
                            dialogTitle = "Select Download Directory"
                            val current = DesktopSettings.imageSaveDir.value
                            if (!current.isNullOrBlank()) {
                                currentDirectory = java.io.File(current)
                            }
                        }
                        if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
                            val selected = chooser.selectedFile.absolutePath
                            saveDirState.setTextAndPlaceCursorAtEnd(selected)
                            DesktopSettings.imageSaveDir.value = selected
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "…",
                    color = MiuixTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (DesktopSettings.imageSaveDir.value.isNullOrBlank()) {
            Text(
                text = "默认: $defaultDir",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
