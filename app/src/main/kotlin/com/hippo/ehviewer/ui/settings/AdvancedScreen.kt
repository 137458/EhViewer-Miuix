package com.hippo.ehviewer.ui.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.ehviewer.core.files.delete
import com.ehviewer.core.files.sendTo
import com.ehviewer.core.files.toOkioPath
import com.ehviewer.core.i18n.R
import com.ehviewer.core.ui.component.BlurredBar
import com.ehviewer.core.ui.component.blurBackdropSource
import com.ehviewer.core.ui.component.rememberBlurBackdrop
import com.ehviewer.core.util.isAtLeastO
import com.ehviewer.core.util.launch
import com.ehviewer.core.util.logcat
import com.ehviewer.core.util.withIOContext
import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.asMutableState
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.data.FavListUrlBuilder
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.ktor.isCronetAvailable
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.main.NavigationIcon
import com.hippo.ehviewer.ui.showRestartDialog
import com.hippo.ehviewer.util.AdsPlaceholderFile
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.CrashHandler
import com.hippo.ehviewer.util.ReadableTime
import com.hippo.ehviewer.util.displayPath
import com.hippo.ehviewer.util.getAppLanguage
import com.hippo.ehviewer.util.getLanguages
import com.hippo.ehviewer.util.setAppLanguage
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge
import moe.tarsin.coroutines.runSuspendCatching
import moe.tarsin.snackbar
import moe.tarsin.string
import com.hippo.ehviewer.ui.tools.awaitConfirmationOrCancel
import com.hippo.ehviewer.util.StorageCleaner
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

context(ctx: Context)
private fun dumplog(uri: Uri): Unit = with(ctx) {
    grantUriPermission(BuildConfig.APPLICATION_ID, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    contentResolver.openOutputStream(uri)?.use { outputStream ->
        val files = ArrayList<File>()
        AppConfig.externalParseErrorDir?.listFiles()?.let { files.addAll(it) }
        AppConfig.externalCrashDir?.listFiles()?.let { files.addAll(it) }
        ZipOutputStream(outputStream).use { zipOs ->
            files.forEach { file ->
                if (!file.isFile) return@forEach
                val entry = ZipEntry(file.name)
                zipOs.putNextEntry(entry)
                file.inputStream().use { it.copyTo(zipOs) }
            }
            val logcatEntry = ZipEntry("logcat-" + ReadableTime.getFilenamableTime() + ".txt")
            zipOs.putNextEntry(logcatEntry)
            CrashHandler.collectInfo(zipOs.writer())
            Runtime.getRuntime().exec("logcat -d").inputStream.use { it.copyTo(zipOs) }
        }
    }
}

context(ctx: Context)
private suspend fun exportDatabase(uri: Uri) {
    ctx.grantUriPermission(BuildConfig.APPLICATION_ID, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    EhDB.exportDB(uri.toOkioPath())
}

context(ctx: Context)
private suspend fun importDatabase(uri: Uri) {
    ctx.grantUriPermission(BuildConfig.APPLICATION_ID, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    EhDB.importDB(uri.toOkioPath())
}

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.AdvancedScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val scrollBehavior = MiuixScrollBehavior()
    val colorScheme = MiuixTheme.colorScheme
    fun launchSnackbar(message: String) = launch { snackbar(message) }

    val backdrop = rememberBlurBackdrop()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
            ) {
                TopAppBar(
                    title = stringResource(id = R.string.settings_advanced),
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
                SmallTitle(text = stringResource(id = R.string.settings_advanced_app_language_title))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    var currentLanguage by remember { mutableStateOf(getAppLanguage()) }
                    val languages = remember { getLanguages() }
                    val languageKeys = remember(languages) { languages.keys.toList() }
                    val languageValues = remember(languages) { languages.values.toList() }
                    val selectedLangIndex = languageKeys.indexOf(currentLanguage).coerceAtLeast(0)
                    OverlayDropdownPreference(
                        title = stringResource(id = R.string.settings_advanced_app_language_title),
                        items = languageValues,
                        selectedIndex = selectedLangIndex,
                        onSelectedIndexChange = { index ->
                            if (index in languageKeys.indices) {
                                val key = languageKeys[index]
                                setAppLanguage(key)
                                currentLanguage = key
                            }
                        },
                    )
                    SwitchPreference(
                        title = stringResource(id = R.string.animate_items),
                        summary = stringResource(id = R.string.animate_items_summary),
                        state = Settings.animateItems.asMutableState(),
                    )
                    SwitchPreference(
                        title = stringResource(id = R.string.desktop_site),
                        summary = stringResource(id = R.string.desktop_site_summary),
                        state = Settings.desktopSite.asMutableState(),
                    )
                    SimpleMenuPreferenceInt(
                        title = stringResource(id = R.string.settings_advanced_read_cache_size),
                        entry = com.hippo.ehviewer.R.array.read_cache_size_entries,
                        entryValueRes = com.hippo.ehviewer.R.array.read_cache_size_entry_values,
                        state = Settings.readCacheSize.asMutableState(),
                    )
                }

                SmallTitle(text = stringResource(id = R.string.settings_advanced_save_crash_log))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    SwitchPreference(
                        title = stringResource(id = R.string.settings_advanced_save_parse_error_body),
                        summary = stringResource(id = R.string.settings_advanced_save_parse_error_body_summary),
                        state = Settings.saveParseErrorBody.asMutableState(),
                    )
                    SwitchPreference(
                        title = stringResource(id = R.string.settings_advanced_save_crash_log),
                        summary = stringResource(id = R.string.settings_advanced_save_crash_log_summary),
                        state = Settings.saveCrashLog.asMutableState(),
                    )
                    val dumpLogError = stringResource(id = R.string.settings_advanced_dump_logcat_failed)
                    LauncherPreference(
                        title = stringResource(id = R.string.settings_advanced_dump_logcat),
                        summary = stringResource(id = R.string.settings_advanced_dump_logcat_summary),
                        contract = ActivityResultContracts.CreateDocument("application/zip"),
                        key = "log-" + ReadableTime.getFilenamableTime() + ".zip",
                    ) { uri ->
                        uri?.run {
                            runCatching {
                                dumplog(uri)
                                launchSnackbar(string(R.string.settings_advanced_dump_logcat_to, uri.displayPath))
                            }.onFailure {
                                launchSnackbar(dumpLogError)
                                logcat(it)
                            }
                        }
                    }
                }

                SmallTitle(text = stringResource(id = R.string.settings_advanced))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    val stripAds = Settings.stripExtraneousAds.asMutableState()
                    SwitchPreference(
                        title = stringResource(id = R.string.settings_block_extraneous_ads),
                        state = stripAds,
                    )
                    AnimatedVisibility(visible = stripAds.value) {
                        LauncherPreference(
                            title = stringResource(id = R.string.settings_ads_placeholder),
                            contract = ActivityResultContracts.PickVisualMedia(),
                            key = PickVisualMediaRequest(mediaType = ImageOnly),
                        ) { uri ->
                            withIOContext {
                                if (uri != null) {
                                    uri.toOkioPath() sendTo AdsPlaceholderFile
                                } else {
                                    AdsPlaceholderFile.delete()
                                }
                            }
                        }
                    }
                    if (isCronetAvailable) {
                        val enableCronet = Settings.enableCronet.asMutableState()
                        if (BuildConfig.DEBUG || !enableCronet.value) {
                            SwitchPreference(
                                title = "Enable Cronet",
                                state = enableCronet,
                            )
                        }
                        AnimatedVisibility(enableCronet.value) {
                            SwitchPreference(
                                title = stringResource(id = R.string.settings_advanced_enable_quic),
                                state = Settings.enableQuic.asMutableState(),
                            )
                        }
                        LaunchedEffect(Unit) {
                            merge(
                                Settings.enableCronet.changesFlow(),
                                Settings.enableQuic.changesFlow(),
                            ).collectLatest {
                                showRestartDialog()
                            }
                        }
                    }
                    if (isAtLeastO) {
                        IntSliderPreference(
                            maxValue = 16384,
                            step = 3,
                            title = stringResource(id = R.string.settings_advanced_hardware_bitmap_threshold),
                            summary = stringResource(id = R.string.settings_advanced_hardware_bitmap_threshold_summary),
                            state = Settings.hardwareBitmapThreshold.asMutableState(),
                        )
                    }
                    SwitchPreference(
                        title = stringResource(id = R.string.preload_thumb_aggressively),
                        state = Settings.preloadThumbAggressively.asMutableState(),
                    )
                }

                SmallTitle(text = stringResource(id = R.string.settings_advanced_export_data))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    val exportFailed = stringResource(id = R.string.settings_advanced_export_data_failed)
                    LauncherPreference(
                        title = stringResource(id = R.string.settings_advanced_export_data),
                        summary = stringResource(id = R.string.settings_advanced_export_data_summary),
                        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
                        key = ReadableTime.getFilenamableTime() + ".db",
                    ) { uri ->
                        uri?.let {
                            runCatching {
                                exportDatabase(uri)
                                launchSnackbar(string(R.string.settings_advanced_export_data_to, uri.displayPath))
                            }.onFailure {
                                logcat(it)
                                launchSnackbar(exportFailed)
                            }
                        }
                    }
                    val importFailed = stringResource(id = R.string.cant_read_the_file)
                    val importSucceed = stringResource(id = R.string.settings_advanced_import_data_successfully)
                    LauncherPreference(
                        title = stringResource(id = R.string.settings_advanced_import_data),
                        summary = stringResource(id = R.string.settings_advanced_import_data_summary),
                        contract = ActivityResultContracts.GetContent(),
                        key = "application/octet-stream",
                    ) { uri ->
                        uri?.let {
                            runCatching {
                                importDatabase(uri)
                                launchSnackbar(importSucceed)
                            }.onFailure {
                                logcat(it)
                                launchSnackbar(importFailed)
                            }
                        }
                    }
                    val hasSignedIn by Settings.hasSignedIn.collectAsState()
                    if (hasSignedIn) {
                        val backupNothing = stringResource(id = R.string.settings_advanced_backup_favorite_nothing)
                        val backupFailed = stringResource(id = R.string.settings_advanced_backup_favorite_failed)
                        val backupSucceed = stringResource(id = R.string.settings_advanced_backup_favorite_success)
                        Preference(
                            title = stringResource(id = R.string.settings_advanced_backup_favorite),
                            summary = stringResource(id = R.string.settings_advanced_backup_favorite_summary),
                        ) {
                            val favListUrlBuilder = FavListUrlBuilder()
                            var favTotal = 0
                            var favIndex = 0
                            tailrec suspend fun doBackup() {
                                val result = EhEngine.getFavorites(favListUrlBuilder.build())
                                if (result.galleryInfoList.isEmpty()) {
                                    launchSnackbar(backupNothing)
                                } else {
                                    if (favTotal == 0) favTotal = result.countArray.sum()
                                    favIndex += result.galleryInfoList.size
                                    val status = "($favIndex/$favTotal)"
                                    EhDB.putLocalFavorites(result.galleryInfoList)
                                    launchSnackbar(string(R.string.settings_advanced_backup_favorite_start, status))
                                    if (result.next != null) {
                                        delay(Settings.downloadDelay.value.toLong())
                                        favListUrlBuilder.setIndex(result.next, true)
                                        doBackup()
                                    }
                                }
                            }
                            launch {
                                runSuspendCatching {
                                    doBackup()
                                }.onSuccess {
                                    launchSnackbar(backupSucceed)
                                }.onFailure {
                                    logcat(it)
                                    launchSnackbar(backupFailed)
                                }
                            }
                        }
                    }
                    Preference(title = stringResource(id = R.string.open_by_default)) {
                        openByDefaultSettings()
                    }
                    WorkPreference(
                        title = stringResource(id = R.string.settings_advanced_deep_clean),
                        summary = stringResource(id = R.string.settings_advanced_deep_clean_summary),
                    ) {
                        awaitConfirmationOrCancel(
                            confirmText = R.string.clear_all,
                            title = R.string.settings_advanced_deep_clean,
                        ) {
                            Text(text = stringResource(id = R.string.settings_advanced_deep_clean_confirm))
                        }
                        val result = StorageCleaner.performDeepClean()
                        launchSnackbar(string(R.string.settings_advanced_deep_clean_done, result.deadDownloadsCount))
                    }
                }
            }
        }
    }
}

context(ctx: Context)
private fun openByDefaultSettings() = with(ctx) {
    try {
        @SuppressLint("InlinedApi")
        val intent = Intent(
            ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
            "package:$packageName".toUri(),
        )
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        val intent = Intent(
            ACTION_APPLICATION_DETAILS_SETTINGS,
            "package:$packageName".toUri(),
        )
        startActivity(intent)
    }
}
