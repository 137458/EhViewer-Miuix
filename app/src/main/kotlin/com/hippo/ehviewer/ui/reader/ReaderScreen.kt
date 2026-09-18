package com.hippo.ehviewer.ui.reader

import android.content.Context
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import arrow.core.Either
import arrow.core.Either.Companion.catch
import arrow.core.raise.ensure
import arrow.core.right
import com.ehviewer.core.i18n.R
import com.ehviewer.core.model.BaseGalleryInfo
import com.ehviewer.core.ui.component.LocalBackdrop
import com.ehviewer.core.ui.component.blurBackdropSource
import com.ehviewer.core.ui.component.rememberBlurBackdrop
import com.ehviewer.core.ui.util.Await
import com.ehviewer.core.ui.util.asyncInVM
import com.ehviewer.core.ui.util.rememberSystemUiController
import com.ehviewer.core.ui.util.thenIf
import com.ehviewer.core.util.launch
import com.ehviewer.core.util.launchIO
import com.ehviewer.core.util.unreachable
import com.ehviewer.core.util.withUIContext
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.ui.screen.asDst
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.download.archiveFile
import com.hippo.ehviewer.gallery.Page
import com.hippo.ehviewer.gallery.PageLoader
import com.hippo.ehviewer.gallery.PageStatus
import com.hippo.ehviewer.gallery.status
import com.hippo.ehviewer.gallery.unblock
import com.hippo.ehviewer.gallery.useArchivePageLoader
import com.hippo.ehviewer.gallery.useEhPageLoader
import com.hippo.ehviewer.ui.MainActivity
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.theme.EhTheme
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.ui.tools.awaitInputText
import com.hippo.ehviewer.ui.tools.dialog
import com.hippo.ehviewer.util.displayString
import com.hippo.ehviewer.util.hasAds
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.ui.reader.PageIndicatorText
import eu.kanade.tachiyomi.ui.reader.ReaderAppBars
import eu.kanade.tachiyomi.ui.reader.ReaderContentOverlay
import eu.kanade.tachiyomi.ui.reader.ReaderPageSheetMeta
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import moe.tarsin.string
import okio.Path.Companion.toPath
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

@Serializable
sealed interface ReaderScreenArgs {
    @Serializable
    data class Gallery(val info: BaseGalleryInfo, val page: Int) : ReaderScreenArgs

    @Serializable
    data class Archive(val path: String) : ReaderScreenArgs
}

@Composable
private fun Background(
    readerBackground: ReaderBackground,
    content: @Composable () -> Unit,
) = Box(Modifier.fillMaxSize().background(readerBackground.color), contentAlignment = Alignment.Center) {
    EhTheme(useDarkTheme = !readerBackground.isLight, content = content)
}

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.ReaderScreen(args: ReaderScreenArgs, navigator: DestinationsNavigator) = Screen(navigator) {
    val readerBackground by collectReaderBackgroundAsState()
    val uiController = rememberSystemUiController()
    DisposableEffect(uiController) {
        val lightStatusBar = uiController.statusBarDarkContentEnabled
        uiController.statusBarDarkContentEnabled = readerBackground.isLight
        onDispose {
            uiController.statusBarDarkContentEnabled = lightStatusBar
        }
    }

    var retryTrigger by remember { mutableIntStateOf(0) }
    Await(
        block = asyncInVM(args to retryTrigger) { alive ->
            suspendCancellableCoroutine { cont ->
                with(alive) {
                    launchIO {
                        catch {
                            usePageLoader(args) { loader ->
                                cont.resume(loader.right())
                                awaitCancellation()
                            }
                        }.let { left -> cont.resume(left) }
                    }
                }
            }
        }.value.run {
            { await() }
        },
        placeholder = {
            Background(readerBackground) {
                InfiniteProgressIndicator()
            }
        },
    ) { result ->
        when (result) {
            is Either.Left -> Background(readerBackground) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = result.value.displayString(),
                        color = MiuixTheme.colorScheme.error,
                        style = MiuixTheme.textStyles.title2,
                        textAlign = TextAlign.Center,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { navigator.popBackStack() },
                        ) {
                            Text(text = stringResource(id = android.R.string.cancel))
                        }
                        Button(
                            onClick = { retryTrigger++ },
                        ) {
                            Text(text = stringResource(id = R.string.action_retry))
                        }
                    }
                }
            }
            is Either.Right -> {
                val loader = result.value
                val info = (args as? ReaderScreenArgs.Gallery)?.info
                key(loader) {
                    ReaderScreen(loader, info)
                }
            }
        }
    }
}

@Composable
context(activity: MainActivity, _: SnackbarHostState, _: DialogState, _: CoroutineScope, nav: DestinationsNavigator)
fun ReaderScreen(pageLoader: PageLoader, info: BaseGalleryInfo?) {
    LaunchedEffect(Unit) {
        val orientation = activity.requestedOrientation
        Settings.orientationMode.valueFlow()
            .onCompletion { activity.requestedOrientation = orientation }
            .collect { activity.setOrientation(it) }
    }
    LaunchedEffect(pageLoader) {
        with(Settings) {
            merge(cropBorder.changesFlow(), stripExtraneousAds.changesFlow()).collect {
                pageLoader.restart()
            }
        }
    }
    val webtoon = remember(info) {
        // Tags in database may or may not have the prefix "other:"
        info?.simpleTags?.any { it.endsWith("webtoon") } == true
    }
    val showSeekbar by Settings.showReaderSeekbar.collectAsState()
    val readingMode by Settings.readingMode.collectAsState {
        when (val mode = ReadingModeType.fromPreference(it)) {
            ReadingModeType.DEFAULT -> if (webtoon) ReadingModeType.WEBTOON else ReadingModeType.RIGHT_TO_LEFT
            else -> mode
        }
    }
    val volumeKeysEnabled by Settings.readWithVolumeKeys.collectAsState()
    val volumeKeysInverted by Settings.readWithVolumeKeysInverted.collectAsState()
    val fullscreen by Settings.fullscreen.collectAsState()
    val cutoutShort by Settings.cutoutShort.collectAsState()
    val keepScreenOn by Settings.keepScreenOn.collectAsState()
    val uiController = rememberSystemUiController()
    DisposableEffect(uiController) {
        uiController.showTransientSystemBarsBySwipe = true
        onDispose {
            uiController.isSystemBarsVisible = true
            uiController.showTransientSystemBarsBySwipe = false
        }
    }
    val lazyListState = rememberLazyListState(LazyLayoutCacheWindow(SCROLL_FRACTION, SCROLL_FRACTION), pageLoader.startPage)
    val pagerState = rememberPagerState(pageLoader.startPage) { pageLoader.size }
    val syncState = rememberSliderPagerDoubleSyncState(lazyListState, pagerState, pageLoader)
    var appbarVisible by remember { mutableStateOf(false) }
    val isWebtoon by rememberUpdatedState(ReadingModeType.isWebtoon(readingMode))
    val readerBackdrop = rememberBlurBackdrop()
    val focusRequester = remember { FocusRequester() }
    CompositionLocalProvider(LocalBackdrop provides readerBackdrop) {
        Box(
            Modifier.keyEventHandler(
                volumeKeysEnabled = { volumeKeysEnabled && !appbarVisible },
                volumeKeysInverted = { volumeKeysInverted },
                movePrevious = { launch { if (isWebtoon) lazyListState.scrollUp() else pagerState.moveToPrevious() } },
                moveNext = { launch { if (isWebtoon) lazyListState.scrollDown() else pagerState.moveToNext() } },
            ).focusRequester(focusRequester).focusable().thenIf(keepScreenOn) { keepScreenOn() },
        ) {
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
            syncState.Sync(isWebtoon) { appbarVisible = false }
            val readerBackground by collectReaderBackgroundAsState()
            val isDarkTheme = isSystemInDarkTheme()
            LaunchedEffect(isDarkTheme) {
                snapshotFlow { appbarVisible }.collect {
                    uiController.isSystemBarsVisible = it || !fullscreen
                    uiController.statusBarDarkContentEnabled = if (it) !isDarkTheme else readerBackground.isLight
                }
            }
            var showNavigationOverlay by remember {
                val showOnStart = Settings.showNavigationOverlayNewUser.value || Settings.showNavigationOverlayOnStart.value
                Settings.showNavigationOverlayNewUser.value = false
                mutableStateOf(showOnStart)
            }
            val onSelectPage = { page: Page ->
                if (Settings.readerLongTapAction.value) {
                    launch {
                        val blocked = page.status is PageStatus.Blocked
                        dialog { cont ->
                            fun dispose() = cont.resume(Unit)
                            WindowBottomSheet(
                                show = true,
                                onDismissRequest = { dispose() },
                            ) {
                                ReaderPageSheetMeta(
                                    retry = { pageLoader.retryPage(page.index) },
                                    retryOrigin = { pageLoader.retryPage(page.index, true) },
                                    share = { launchIO { with(pageLoader) { shareImage(page, info) } } },
                                    copy = { launchIO { with(pageLoader) { copy(page) } } },
                                    save = { launchIO { with(pageLoader) { save(page) } } },
                                    saveTo = { launchIO { with(pageLoader) { saveTo(page) } } },
                                    searchByImage = {
                                        launchIO {
                                            with(pageLoader) {
                                                searchByImage(page) { hash ->
                                                    withUIContext {
                                                        nav.navigate(
                                                            ListUrlBuilder(
                                                                mode = ListUrlBuilder.MODE_IMAGE_SEARCH,
                                                                hash = hash,
                                                            ).asDst(),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    showAds = { page.unblock() }.takeIf { blocked },
                                    dismiss = { dispose() },
                                )
                            }
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blurBackdropSource(readerBackdrop),
            ) {
                EhTheme(useDarkTheme = !readerBackground.isLight) {
                    val insets = if (fullscreen) {
                        if (cutoutShort) {
                            WindowInsets()
                        } else {
                            WindowInsets.displayCutout
                        }
                    } else {
                        WindowInsets.systemBars
                    }
                    GalleryPager(
                        type = readingMode,
                        pagerState = pagerState,
                        lazyListState = lazyListState,
                        pageLoader = pageLoader,
                        showNavigationOverlay = showNavigationOverlay,
                        onNavigationModeChange = { showNavigationOverlay = true },
                        onSelectPage = onSelectPage,
                        onMenuRegionClick = { appbarVisible = !appbarVisible },
                        modifier = Modifier.background(readerBackground.color).pointerInput(syncState) {
                            awaitEachGesture {
                                waitForUpOrCancellation()
                                syncState.reset()
                                showNavigationOverlay = false
                            }
                        }.fillMaxSize().windowInsetsPadding(insets),
                    )
                }
            }
            val brightness by Settings.customBrightness.collectAsState()
            val brightnessValue by Settings.customBrightnessValue.collectAsState()
            val colorOverlayEnabled by Settings.colorFilter.collectAsState()
            val colorOverlay by Settings.colorFilterValue.collectAsState()
            val colorOverlayMode by Settings.colorFilterMode.collectAsState {
                when (it) {
                    0 -> BlendMode.SrcOver
                    1 -> BlendMode.Multiply
                    2 -> BlendMode.Screen
                    3 -> BlendMode.Overlay
                    4 -> BlendMode.Lighten
                    5 -> BlendMode.Darken
                    else -> unreachable()
                }
            }
            ReaderContentOverlay(
                brightness = { brightnessValue }.takeIf { brightness && brightnessValue < 0 },
                color = { colorOverlay }.takeIf { colorOverlayEnabled },
                colorBlendMode = colorOverlayMode,
            )
            if (brightness) {
                LaunchedEffect(Unit) {
                    Settings.customBrightnessValue.valueFlow().sample(100)
                        .onCompletion { activity.setCustomBrightnessValue(0) }
                        .collect { activity.setCustomBrightnessValue(it) }
                }
            }
            val showPageNumber by Settings.showPageNumber.collectAsState()
            if (showPageNumber && !appbarVisible) {
                PageIndicatorText(
                    currentPage = syncState.sliderValue,
                    totalPages = pageLoader.size,
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
                )
            }
            ReaderAppBars(
                visible = appbarVisible,
                title = pageLoader.title,
                isRtl = readingMode == ReadingModeType.RIGHT_TO_LEFT,
                showSeekBar = showSeekbar,
                currentPage = syncState.sliderValue,
                totalPages = pageLoader.size,
                onSliderValueChange = syncState::sliderScrollTo,
                onClickSettings = {
                    launch {
                        dialog { cont ->
                            fun dispose() = cont.resume(Unit)
                            var isColorFilter by remember { mutableStateOf(false) }
                            WindowBottomSheet(
                                show = true,
                                onDismissRequest = { dispose() },
                                enableWindowDim = !isColorFilter,
                            ) {
                                SettingsPager(isWebtoon = isWebtoon, modifier = Modifier.fillMaxSize()) { page ->
                                    isColorFilter = page == 2
                                    appbarVisible = !isColorFilter
                                }
                            }
                        }
                    }
                },
            )
        }
    }
}

context(_: Context, _: DialogState, nav: DestinationsNavigator)
suspend inline fun <T> usePageLoader(args: ReaderScreenArgs, crossinline block: suspend (PageLoader) -> T) = when (args) {
    is ReaderScreenArgs.Gallery -> {
        val info = args.info
        val page = args.page.takeUnless { it == -1 } ?: EhDB.getReadProgress(info.gid)
        val archive = DownloadManager.getDownloadInfo(info.gid)?.archiveFile
        if (archive != null) {
            useArchivePageLoader(archive, info, page, info.hasAds, { error("Managed Archive have password???") }, block)
        } else {
            useEhPageLoader(info, page, block)
        }
    }
    is ReaderScreenArgs.Archive -> useArchivePageLoader(
        args.path.toPath(),
        passwdProvider = { invalidator ->
            awaitInputText(
                title = string(R.string.archive_need_passwd),
                hint = string(R.string.archive_passwd),
                onUserDismiss = { nav.popBackStack() },
            ) { text ->
                ensure(text.isNotBlank()) { string(R.string.passwd_cannot_be_empty) }
                ensure(invalidator(text)) { string(R.string.passwd_wrong) }
            }
        },
        block = block,
    )
}

/**
 * Reader surface colour plus its light/dark classification.
 *
 * The light/dark decision drives [EhTheme] and the status-bar icon colour, so it is carried
 * explicitly rather than inferred from the colour value: comparing the background against
 * [Color.White] would silently mis-classify any future off-white reader theme.
 */
private data class ReaderBackground(val color: Color, val isLight: Boolean)

@Composable
private fun collectReaderBackgroundAsState(): State<ReaderBackground> {
    val grey = colorResource(com.hippo.ehviewer.R.color.reader_background_dark)
    val dark = isSystemInDarkTheme()
    return Settings.readerTheme.collectAsState { theme ->
        when (theme) {
            0 -> ReaderBackground(Color.White, isLight = true)
            2 -> ReaderBackground(grey, isLight = false)
            3 -> if (dark) ReaderBackground(grey, isLight = false) else ReaderBackground(Color.White, isLight = true)
            else -> ReaderBackground(Color.Black, isLight = false)
        }
    }
}
