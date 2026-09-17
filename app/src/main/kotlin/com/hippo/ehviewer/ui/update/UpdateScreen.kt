package com.hippo.ehviewer.ui.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.ehviewer.core.i18n.R
import com.ehviewer.core.ui.component.BlurredBar
import com.ehviewer.core.ui.component.SquircleShape
import com.ehviewer.core.ui.component.blurBackdropSource
import com.ehviewer.core.ui.component.rememberBlurBackdrop
import com.ehviewer.core.ui.util.LocalBottomBarContentPadding
import com.ehviewer.core.util.launch
import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.asMutableState
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.component.MarkdownText
import com.hippo.ehviewer.ui.main.NavigationIcon
import com.hippo.ehviewer.ui.settings.Preference
import com.hippo.ehviewer.ui.settings.SimpleMenuPreferenceInt
import com.hippo.ehviewer.ui.settings.SwitchPreference
import com.hippo.ehviewer.ui.settings.UrlPreference
import com.hippo.ehviewer.updater.AppUpdater
import com.hippo.ehviewer.updater.Release
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.FileUtils
import com.hippo.ehviewer.util.displayString
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.tarsin.coroutines.runSuspendCatching
import moe.tarsin.snackbar
import moe.tarsin.string
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val REPO_URL = "https://github.com/${BuildConfig.REPO_NAME}"
private const val RELEASES_URL = "$REPO_URL/releases"
private const val ACTIONS_URL = "$REPO_URL/actions"

/**
 * Xiaomi HyperOS 3.0 / Miuix 旗舰级规范软件更新页面。
 * 具备动态流光背景、Hero 双层弥散悬浮应用大徽标、多态胶囊浮岛、
 * 杂志感 Markdown 详细更新日志、多维通道设置、以及全功能沙盒模拟测试入口。
 */
@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.UpdateScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val scrollBehavior = MiuixScrollBehavior()
    val colorScheme = MiuixTheme.colorScheme
    val backdrop = rememberBlurBackdrop()
    val context = LocalContext.current
    val appIcon = remember(context) {
        runCatching {
            context.packageManager.getApplicationIcon(context.packageName).toBitmap().asImageBitmap()
        }.getOrNull()
    }

    fun launchSnackbar(message: String) = launch { snackbar(message) }

    var isChecking by remember { mutableStateOf(false) }
    var updateRelease by remember { mutableStateOf<Release?>(null) }
    var checkError by remember { mutableStateOf<String?>(null) }
    var hasChecked by remember { mutableStateOf(false) }

    var showDialog by remember { mutableStateOf(false) }
    var isDialogSimulated by remember { mutableStateOf(false) }
    var dialogRelease by remember { mutableStateOf<Release?>(null) }

    fun performCheck() {
        if (isChecking) return
        isChecking = true
        checkError = null
        launch {
            runSuspendCatching {
                withContext(Dispatchers.IO) {
                    AppUpdater.checkForUpdate(forceCheck = true)
                }
            }.onSuccess { release ->
                isChecking = false
                hasChecked = true
                updateRelease = release
                if (release == null) {
                    launchSnackbar(string(R.string.already_latest_version))
                } else {
                    dialogRelease = release
                    isDialogSimulated = false
                    showDialog = true
                }
            }.onFailure { e ->
                isChecking = false
                hasChecked = true
                checkError = e.displayString()
                launchSnackbar(string(R.string.update_failed, e.displayString()))
            }
        }
    }

    fun triggerSandboxSimulation() {
        val mock = AppUpdater.generateMockRelease()
        dialogRelease = mock
        isDialogSimulated = true
        showDialog = true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
            ) {
                TopAppBar(
                    title = stringResource(id = R.string.update_screen_title),
                    navigationIcon = { NavigationIcon() },
                    scrollBehavior = scrollBehavior,
                    color = if (backdrop != null) Color.Transparent else colorScheme.surface,
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface)
                .blurBackdropSource(backdrop),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues),
            ) {
                // ── Hero 区域：发光悬浮徽标 + 状态胶囊浮岛 ──
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // 双层弥散发光应用大图标
                    Box(
                        modifier = Modifier.size(96.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        // 底层光晕扩散层
                        Box(
                            modifier = Modifier
                                .size(86.dp)
                                .shadow(elevation = 16.dp, shape = RoundedCornerShape(26.dp), spotColor = colorScheme.primary)
                                .background(colorScheme.primary.copy(alpha = 0.18f), RoundedCornerShape(26.dp)),
                        )
                        // 顶层图标容器
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(SquircleShape(20.dp))
                                .border(1.dp, colorScheme.dividerLine.copy(alpha = 0.25f), SquircleShape(20.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (appIcon != null) {
                                Image(
                                    bitmap = appIcon,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 应用主标题
                        Text(
                            text = "EhViewer",
                            style = MiuixTheme.textStyles.title2.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                            ),
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 当前运行版本与 Commit
                        Text(
                            text = "v${BuildConfig.RAW_VERSION_NAME} (${BuildConfig.COMMIT_SHA})",
                            style = MiuixTheme.textStyles.body2.copy(fontSize = 13.sp),
                            color = colorScheme.onSurfaceVariantSummary,
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 状态胶囊浮岛 (Status Capsule Island)
                        val statusModifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)

                        when {
                            isChecking -> {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                        .then(statusModifier),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = stringResource(R.string.update_checking_hint),
                                        style = MiuixTheme.textStyles.body2.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                        ),
                                        color = colorScheme.onSurfaceVariantSummary,
                                    )
                                }
                            }
                            updateRelease != null -> {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(colorScheme.primary.copy(alpha = 0.12f))
                                        .border(1.dp, colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                                        .then(statusModifier),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(colorScheme.primary),
                                    )
                                    Text(
                                        text = stringResource(R.string.update_found_header, updateRelease!!.version, BuildConfig.RAW_VERSION_NAME),
                                        style = MiuixTheme.textStyles.body2.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                        color = colorScheme.primary,
                                    )
                                }
                            }
                            checkError != null -> {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(colorScheme.error.copy(alpha = 0.10f))
                                        .border(1.dp, colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                        .then(statusModifier),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(colorScheme.error),
                                    )
                                    Text(
                                        text = stringResource(R.string.update_failed, checkError ?: ""),
                                        style = MiuixTheme.textStyles.body2.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                        ),
                                        color = colorScheme.error,
                                    )
                                }
                            }
                            else -> {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                        .then(statusModifier),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(colorScheme.primary.copy(alpha = 0.8f)),
                                    )
                                    Text(
                                        text = if (hasChecked) {
                                            stringResource(R.string.update_latest_header, BuildConfig.RAW_VERSION_NAME)
                                        } else {
                                            stringResource(R.string.already_latest_version)
                                        },
                                        style = MiuixTheme.textStyles.body2.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                        ),
                                        color = colorScheme.onSurfaceVariantSummary,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Hero 主快捷操作按钮
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth(0.92f),
                        ) {
                            if (updateRelease != null) {
                                Button(
                                    onClick = {
                                        dialogRelease = updateRelease
                                        isDialogSimulated = false
                                        showDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColorsPrimary(),
                                    modifier = Modifier.weight(1.3f),
                                ) {
                                    Text(stringResource(R.string.update_btn_download_now))
                                }
                                Button(
                                    onClick = { performCheck() },
                                    colors = ButtonDefaults.buttonColors(),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(stringResource(R.string.update_dialog_btn_retry))
                                }
                            } else {
                                Button(
                                    onClick = { performCheck() },
                                    enabled = !isChecking,
                                    colors = ButtonDefaults.buttonColorsPrimary(),
                                    modifier = Modifier.weight(1.2f),
                                ) {
                                    Text(
                                        if (isChecking) {
                                            stringResource(R.string.update_checking_hint)
                                        } else {
                                            stringResource(R.string.update_pref_manual_check_title)
                                        },
                                    )
                                }
                                Button(
                                    onClick = { triggerSandboxSimulation() },
                                    colors = ButtonDefaults.buttonColors(),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(stringResource(R.string.update_pref_test_dialog_title))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // ── 卡片 1：新版本特性与更新日志（当检出更新时高亮呈现） ──
                    if (updateRelease != null) {
                        val rel = updateRelease!!
                        SmallTitle(text = stringResource(R.string.update_changelog_title_new, rel.version))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = rel.releaseTitle.ifBlank { stringResource(R.string.update_release_default_title) },
                                        style = MiuixTheme.textStyles.title4.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.primary,
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(colorScheme.primary.copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                    ) {
                                        val chanText = if (rel.isCI) stringResource(R.string.update_channel_ci) else stringResource(R.string.update_channel_official)
                                        Text(
                                            text = chanText,
                                            style = MiuixTheme.textStyles.body2.copy(fontSize = 11.sp),
                                            color = colorScheme.primary,
                                        )
                                    }
                                }

                                if (rel.apkSize > 0L || rel.publishedAt.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (rel.apkSize > 0L) {
                                            Text(
                                                text = FileUtils.humanReadableByteCount(rel.apkSize),
                                                style = MiuixTheme.textStyles.body2.copy(fontSize = 11.sp),
                                                color = colorScheme.onSurfaceVariantSummary,
                                            )
                                        }
                                        if (rel.publishedAt.isNotBlank()) {
                                            Text(
                                                text = rel.publishedAt,
                                                style = MiuixTheme.textStyles.body2.copy(fontSize = 11.sp),
                                                color = colorScheme.onSurfaceVariantSummary,
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                color = colorScheme.dividerLine.copy(alpha = 0.25f),
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                            ) {
                                MarkdownText(
                                    markdown = rel.changelog,
                                    modifier = Modifier.fillMaxWidth(),
                                    baseFontSize = 13,
                                )
                            }
                        }
                    }

                    // ── 卡片 2：更新设置 ──
                    SmallTitle(text = stringResource(R.string.update_section_settings))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        SwitchPreference(
                            title = stringResource(id = R.string.backup_before_update),
                            state = Settings.backupBeforeUpdate.asMutableState(),
                        )
                        SwitchPreference(
                            title = stringResource(id = R.string.use_ci_update_channel),
                            state = Settings.useCIUpdateChannel.asMutableState(),
                        )
                        SimpleMenuPreferenceInt(
                            title = stringResource(id = R.string.auto_updates),
                            entry = com.hippo.ehviewer.R.array.update_frequency,
                            entryValueRes = com.hippo.ehviewer.R.array.update_frequency_values,
                            state = Settings.updateIntervalDays.asMutableState(),
                        )
                        val ignored = Settings.ignoredUpdateVersion.value
                        if (ignored != null) {
                            Preference(
                                title = stringResource(R.string.update_pref_ignore_title),
                                summary = stringResource(R.string.update_pref_ignore_ignored, ignored) + stringResource(R.string.update_pref_restore_summary),
                                onClick = {
                                    Settings.ignoredUpdateVersion.value = null
                                    launchSnackbar(context.getString(R.string.update_msg_restore_alert, ignored))
                                },
                            )
                        }
                    }

                    // ── 卡片 3：版本通道与沙盒操作 ──
                    SmallTitle(text = stringResource(R.string.update_section_channel))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        Preference(
                            title = stringResource(R.string.update_pref_manual_check_title),
                            summary = if (isChecking) {
                                stringResource(R.string.update_pref_manual_check_busy)
                            } else {
                                stringResource(R.string.update_pref_manual_check_idle)
                            },
                            onClick = { performCheck() },
                        )
                        Preference(
                            title = stringResource(R.string.update_pref_test_dialog_title),
                            summary = stringResource(R.string.update_pref_test_dialog_summary),
                            onClick = { triggerSandboxSimulation() },
                        )
                        UrlPreference(
                            title = stringResource(R.string.update_pref_github_releases),
                            url = RELEASES_URL,
                        )
                        UrlPreference(
                            title = stringResource(R.string.update_pref_github_ci),
                            url = ACTIONS_URL,
                        )
                        UrlPreference(
                            title = stringResource(R.string.settings_about_source),
                            url = REPO_URL,
                        )
                    }

                    // ── 卡片 4：环境与构建详情 ──
                    SmallTitle(text = stringResource(R.string.settings_about_version))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        Preference(
                            title = stringResource(R.string.update_build_version_name),
                            summary = "${BuildConfig.VERSION_NAME} (${BuildConfig.RAW_VERSION_NAME})",
                        )
                        Preference(
                            title = stringResource(R.string.update_build_commit_sha),
                            summary = BuildConfig.COMMIT_SHA,
                        )
                        Preference(
                            title = stringResource(R.string.update_build_time),
                            summary = AppConfig.commitTime,
                        )
                    }

                    Spacer(modifier = Modifier.height(LocalBottomBarContentPadding.current + 24.dp))
                }
            }
        }

        // ── 挂载全功能 Miuix UpdateDialog ──
        if (showDialog && dialogRelease != null) {
            UpdateDialog(
                show = showDialog,
                release = dialogRelease!!,
                onDismiss = { showDialog = false },
                onIgnore = if (!isDialogSimulated) {
                    { ver ->
                        Settings.ignoredUpdateVersion.value = ver
                        showDialog = false
                        launchSnackbar(context.getString(R.string.update_msg_ignore_alert, ver))
                    }
                } else {
                    null
                },
                isSimulated = isDialogSimulated,
            )
        }
    }
}
