package com.hippo.ehviewer.ui.update

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.ehviewer.core.i18n.R
import com.ehviewer.core.ui.component.BlurredBar
import com.ehviewer.core.ui.component.blurBackdropSource
import com.ehviewer.core.ui.component.rememberBlurBackdrop
import com.ehviewer.core.ui.effect.BgEffectBackground
import com.ehviewer.core.ui.effect.isRuntimeShaderSupported
import com.ehviewer.core.ui.util.LocalBottomBarContentPadding
import com.ehviewer.core.util.launch
import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.asMutableState
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.component.MarkdownText
import com.hippo.ehviewer.ui.main.NavigationIcon
import com.hippo.ehviewer.ui.openBrowser
import com.hippo.ehviewer.ui.settings.Preference
import com.hippo.ehviewer.ui.settings.SimpleMenuPreferenceInt
import com.hippo.ehviewer.ui.settings.SwitchPreference
import com.hippo.ehviewer.updater.AppUpdater
import com.hippo.ehviewer.updater.Release
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.displayString
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.tarsin.coroutines.runSuspendCatching
import moe.tarsin.snackbar
import moe.tarsin.string
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val REPO_URL = "https://github.com/${BuildConfig.REPO_NAME}"
private const val RELEASES_URL = "$REPO_URL/releases"
private const val ACTIONS_URL = "$REPO_URL/actions"

/**
 * 官方 Miuix / HyperOS 视觉规范系统与应用更新页。
 * 遵循 Xiaomi HyperOS 规范 Hero 滚动视差层级架构与卡片系统。
 */
@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.UpdateScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val lazyListState = rememberLazyListState()
    val context = LocalContext.current
    val backdrop = rememberBlurBackdrop()

    val appIcon = remember(context) {
        runCatching {
            context.packageManager.getApplicationIcon(context.packageName).toBitmap().asImageBitmap()
        }.getOrNull()
    }

    fun launchSnackbar(message: String) = launch { snackbar(message) }

    var isChecking by remember { mutableStateOf(false) }
    var updateRelease by remember { mutableStateOf<Release?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogRelease by remember { mutableStateOf<Release?>(null) }

    fun performCheck(userInitiated: Boolean = false, forceCheck: Boolean = userInitiated) {
        if (isChecking) return
        isChecking = true
        launch {
            runSuspendCatching {
                withContext(Dispatchers.IO) {
                    AppUpdater.checkForUpdate(forceCheck = forceCheck)
                }
            }.onSuccess { release ->
                isChecking = false
                updateRelease = release
                if (release != null) {
                    val ignored = Settings.ignoredUpdateVersion.value
                    if (userInitiated || release.version != ignored) {
                        dialogRelease = release
                        showDialog = true
                    }
                } else if (userInitiated) {
                    launchSnackbar(string(R.string.already_latest_version))
                }
            }.onFailure { e ->
                isChecking = false
                if (userInitiated) {
                    launchSnackbar(string(R.string.update_failed, e.displayString()))
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        performCheck(userInitiated = false, forceCheck = true)
    }

    val scrollProgress by remember {
        derivedStateOf {
            when {
                lazyListState.firstVisibleItemIndex > 0 -> 1f
                else -> {
                    val spacer = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "logoSpacer" }
                    if (spacer != null && spacer.size > 0) {
                        (lazyListState.firstVisibleItemScrollOffset.toFloat() / spacer.size).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                }
            }
        }
    }

    val density = LocalDensity.current
    var logoHeightDp by remember { mutableStateOf(200.dp) }
    val colorScheme = MiuixTheme.colorScheme

    Scaffold(
        topBar = {
            val barColor = if (backdrop != null) {
                Color.Transparent
            } else if (scrollProgress == 1f) {
                colorScheme.surface
            } else {
                Color.Transparent
            }
            val titleColor = colorScheme.onSurface.copy(
                alpha = ((scrollProgress - 0.35f) / 0.65f).coerceIn(0f, 1f),
            )
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = topAppBarScrollBehavior,
            ) {
                SmallTopAppBar(
                    title = stringResource(R.string.update_screen_title),
                    scrollBehavior = topAppBarScrollBehavior,
                    color = barColor,
                    titleColor = titleColor,
                    navigationIcon = { NavigationIcon() },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blurBackdropSource(backdrop),
            contentAlignment = Alignment.TopCenter,
        ) {
            BgEffectBackground(
                dynamicBackground = isRuntimeShaderSupported(),
                isOs3Effect = true,
                isFullSize = true,
                modifier = Modifier.fillMaxSize(),
                alpha = { 1f - scrollProgress },
            ) {
                // ── 顶部官方规范 Hero 视觉 ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = innerPadding.calculateTopPadding() + 24.dp)
                        .onSizeChanged { size ->
                            with(density) { logoHeightDp = size.height.toDp() }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(88.dp)
                            .graphicsLayer {
                                val iconProgress = ((scrollProgress - 0.35f) / 0.15f).coerceIn(0f, 1f)
                                clip = true
                                shape = RoundedCornerShape(24.dp)
                                alpha = 1 - iconProgress
                                scaleX = 1 - (iconProgress * 0.05f)
                                scaleY = 1 - (iconProgress * 0.05f)
                            }
                            .background(colorScheme.surfaceVariant),
                    ) {
                        if (appIcon != null) {
                            Image(
                                bitmap = appIcon,
                                contentDescription = "EhViewer",
                                modifier = Modifier.size(56.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "EhViewer",
                        style = MiuixTheme.textStyles.title2.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface,
                        modifier = Modifier
                            .graphicsLayer {
                                val nameProgress = ((scrollProgress - 0.20f) / 0.15f).coerceIn(0f, 1f)
                                alpha = 1 - nameProgress
                                scaleX = 1 - (nameProgress * 0.05f)
                                scaleY = 1 - (nameProgress * 0.05f)
                            },
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (isChecking) {
                        Text(
                            text = stringResource(R.string.update_checking_hint),
                            color = colorScheme.onSurfaceVariantSummary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    val verProgress = ((scrollProgress - 0.05f) / 0.15f).coerceIn(0f, 1f)
                                    alpha = 1 - verProgress
                                },
                        )
                    } else if (updateRelease != null) {
                        Text(
                            text = stringResource(R.string.update_found_header, updateRelease!!.version, BuildConfig.RAW_VERSION_NAME),
                            color = colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    val verProgress = ((scrollProgress - 0.05f) / 0.15f).coerceIn(0f, 1f)
                                    alpha = 1 - verProgress
                                },
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.update_latest_header, BuildConfig.RAW_VERSION_NAME),
                            color = colorScheme.onSurfaceVariantSummary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    val verProgress = ((scrollProgress - 0.05f) / 0.15f).coerceIn(0f, 1f)
                                    alpha = 1 - verProgress
                                },
                        )
                    }
                }

                // ── 滚动内容列表 ──
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 760.dp)
                        .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding() + 24.dp,
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    item(key = "logoSpacer") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(logoHeightDp + 48.dp),
                        )
                    }

                    // ── 更新日志卡片（检出新版本时常驻展示） ──
                    if (updateRelease != null) {
                        val rel = updateRelease!!
                        item(key = "changelog") {
                            SmallTitle(text = stringResource(R.string.update_changelog_title_new, rel.version))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = rel.releaseTitle.ifBlank { stringResource(R.string.update_release_default_title) },
                                        style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onSurface,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    MarkdownText(
                                        markdown = rel.changelog,
                                        modifier = Modifier.fillMaxWidth(),
                                        baseFontSize = 14,
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    TextButton(
                                        text = stringResource(R.string.update_btn_download_now),
                                        onClick = {
                                            dialogRelease = rel
                                            showDialog = true
                                        },
                                        colors = ButtonDefaults.textButtonColorsPrimary(),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // ── 更新设置 ──
                    item(key = "settings") {
                        SmallTitle(text = stringResource(R.string.update_section_settings))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
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
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // ── 版本通道与操作 ──
                    item(key = "channel") {
                        SmallTitle(text = stringResource(R.string.update_section_channel))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                        ) {
                            BasicComponent(
                                title = stringResource(R.string.update_pref_manual_check_title),
                                summary = if (isChecking) {
                                    stringResource(R.string.update_pref_manual_check_busy)
                                } else {
                                    stringResource(R.string.update_pref_manual_check_idle)
                                },
                                onClick = { performCheck(userInitiated = true) },
                                endActions = {
                                    if (isChecking) {
                                        InfiniteProgressIndicator(modifier = Modifier.size(16.dp))
                                    }
                                },
                            )
                            BasicComponent(
                                title = stringResource(R.string.update_pref_github_releases),
                                summary = stringResource(R.string.update_pref_github_releases_summary),
                                onClick = { with(context) { openBrowser(RELEASES_URL) } },
                            )
                            BasicComponent(
                                title = stringResource(R.string.update_pref_github_ci),
                                summary = stringResource(R.string.update_pref_github_ci_summary),
                                onClick = { with(context) { openBrowser(ACTIONS_URL) } },
                            )
                            BasicComponent(
                                title = stringResource(R.string.settings_about_source),
                                summary = REPO_URL,
                                onClick = { with(context) { openBrowser(REPO_URL) } },
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // ── 构建与版本详情 ──
                    item(key = "build_info") {
                        SmallTitle(text = stringResource(R.string.settings_about_version))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
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
                    }

                    item(key = "bottomPadding") {
                        Spacer(modifier = Modifier.height(LocalBottomBarContentPadding.current + 24.dp))
                    }
                }
            }
        }

        // ── 保持现有效果的更新弹窗 ──
        if (showDialog && dialogRelease != null) {
            UpdateDialog(
                show = showDialog,
                release = dialogRelease!!,
                onDismiss = { showDialog = false },
                onIgnore = { ver ->
                    Settings.ignoredUpdateVersion.value = ver
                    showDialog = false
                    launchSnackbar(context.getString(R.string.update_msg_ignore_alert, ver))
                },
            )
        }
    }
}
