package com.ehviewer.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.TextUnit
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
    val closeToTray by DesktopSettings.closeToTray.valueFlow().collectAsState(DesktopSettings.closeToTray.value)
    val blackDarkTheme by DesktopSettings.blackDarkTheme.valueFlow().collectAsState(DesktopSettings.blackDarkTheme.value)
    val restoreSession by DesktopSettings.restoreSession.valueFlow().collectAsState(DesktopSettings.restoreSession.value)
    val restoreLimit by DesktopSettings.restoreLimit.valueFlow().collectAsState(DesktopSettings.restoreLimit.value)
    val directionRaw by DesktopSettings.readingDirection.valueFlow().collectAsState(DesktopSettings.readingDirection.value)
    val direction = DesktopReadingDirection.fromPersisted(directionRaw)

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
                            contentDescription = stringResource(MR.strings.desktop_a11y_back),
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
                        // 三态显式选择替代循环点击：目标状态可见可直达，选中行以 ✓ 标识
                        SelectionRow(
                            title = stringResource(MR.strings.theme_follow_system),
                            selected = themeMode == 0,
                            onClick = { DesktopSettings.themeMode.value = 0 },
                        )
                        SelectionRow(
                            title = stringResource(MR.strings.theme_light),
                            selected = themeMode == 1,
                            onClick = { DesktopSettings.themeMode.value = 1 },
                        )
                        SelectionRow(
                            title = stringResource(MR.strings.theme_dark),
                            selected = themeMode == 2,
                            onClick = { DesktopSettings.themeMode.value = 2 },
                        )
                        SwitchPreference(
                            title = stringResource(MR.strings.black_dark_theme),
                            checked = blackDarkTheme,
                            onCheckedChange = { DesktopSettings.blackDarkTheme.value = it },
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                        )
                        // 关闭行为显式开关：开 = 最小化到系统托盘，关 = 直接退出（替代点击循环切换）
                        SwitchPreference(
                            title = stringResource(MR.strings.settings_close_minimize_to_tray),
                            checked = closeToTray,
                            onCheckedChange = { DesktopSettings.closeToTray.value = it },
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                        )
                        SwitchPreference(
                            title = stringResource(MR.strings.settings_restore_session),
                            checked = restoreSession,
                            onCheckedChange = { DesktopSettings.restoreSession.value = it },
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                        )
                        if (restoreSession) {
                            // 会话恢复数量显式三选（5/10/20），替代点击循环
                            listOf(5, 10, 20).forEach { limit ->
                                SelectionRow(
                                    title = limit.toString(),
                                    selected = DesktopPageStack.sanitizeRestoreLimit(restoreLimit) == limit,
                                    onClick = { DesktopSettings.restoreLimit.value = limit },
                                )
                            }
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
                        // 两态显式选择替代点击翻转，与主题/关闭行为交互范式一致
                        SelectionRow(
                            title = stringResource(MR.strings.settings_reading_direction_ltr),
                            selected = direction == DesktopReadingDirection.LTR,
                            onClick = { DesktopSettings.readingDirection.value = DesktopReadingDirection.LTR.name },
                        )
                        SelectionRow(
                            title = stringResource(MR.strings.settings_reading_direction_rtl),
                            selected = direction == DesktopReadingDirection.RTL,
                            onClick = { DesktopSettings.readingDirection.value = DesktopReadingDirection.RTL.name },
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
                SettingsFieldActionButton(
                    text = "✕",
                    fontSize = 11.sp,
                    textColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    onClick = {
                        proxyState.setTextAndPlaceCursorAtEnd("")
                        DesktopSettings.proxy.value = null
                    },
                )
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
    val chooserTitle = stringResource(MR.strings.desktop_select_download_directory)
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
            SettingsFieldActionButton(
                text = "…",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textColor = MiuixTheme.colorScheme.primary,
                onClick = {
                    val chooser = javax.swing.JFileChooser().apply {
                        fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
                        dialogTitle = chooserTitle
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
                },
            )
        }
        if (DesktopSettings.imageSaveDir.value.isNullOrBlank()) {
            Text(
                text = stringResource(MR.strings.desktop_download_dir_default, defaultDir),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

// 设置字段行内操作胶囊（代理清空 / 目录选择）：悬停 primary 半透明衬底，与全局操作按钮 token 一致
@Composable
private fun SettingsFieldActionButton(
    text: String,
    fontSize: TextUnit,
    textColor: Color,
    fontWeight: FontWeight? = null,
    onClick: () -> Unit,
) {
    DesktopHoverPill(
        onClick = onClick,
        containerColor = MiuixTheme.colorScheme.surfaceContainerHighest,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
    ) { _ ->
        Text(
            text = text,
            color = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
        )
    }
}

// 通用选择行：选中行尾部 ✓ 标识（BasicComponent endActions 插槽），点击直达目标值
@Composable
private fun SelectionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    BasicComponent(
        title = title,
        onClick = onClick,
        endActions = {
            if (selected) {
                Text(
                    text = "✓",
                    color = MiuixTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
    )
}
