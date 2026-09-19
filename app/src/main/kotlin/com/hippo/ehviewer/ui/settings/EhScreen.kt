package com.hippo.ehviewer.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import com.ehviewer.core.i18n.R
import com.ehviewer.core.network.EhCookieStore
import com.ehviewer.core.ui.component.BlurredBar
import com.ehviewer.core.ui.component.blurBackdropSource
import com.ehviewer.core.ui.component.rememberBlurBackdrop
import com.ehviewer.core.ui.util.readableWidth
import com.ehviewer.core.util.isAtLeastT
import com.ehviewer.core.util.launch
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.asMutableState
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.destinations.FilterScreenDestination
import com.hippo.ehviewer.ui.destinations.MyTagsScreenDestination
import com.hippo.ehviewer.ui.destinations.UConfigScreenDestination
import com.hippo.ehviewer.ui.main.NavigationIcon
import com.hippo.ehviewer.ui.tools.awaitConfirmationOrCancel
import com.hippo.ehviewer.ui.tools.awaitSelectItem
import com.hippo.ehviewer.ui.tools.awaitSelectTime
import com.hippo.ehviewer.util.copyTextToClipboard
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.datetime.LocalTime
import moe.tarsin.navigate
import moe.tarsin.snackbar
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.EhScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val scrollBehavior = MiuixScrollBehavior()
    fun launchSnackBar(content: String) = launch { snackbar(content) }
    val hasSignedIn by Settings.hasSignedIn.collectAsState()
    val backdrop = rememberBlurBackdrop()
    Scaffold(
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
            ) {
                TopAppBar(
                    title = stringResource(id = R.string.settings_eh),
                    navigationIcon = { NavigationIcon() },
                    scrollBehavior = scrollBehavior,
                    color = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface,
                )
            }
        },
    ) { paddingValues ->
        val copiedToClipboard = stringResource(id = R.string.copied_to_clipboard)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background)
                .blurBackdropSource(backdrop),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .readableWidth()
                    .fillMaxHeight()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues),
            ) {
                if (hasSignedIn) {
                    val displayName by Settings.displayName.collectAsState()
                    SmallTitle(text = stringResource(id = R.string.account_name))
                    Card(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Preference(
                            title = stringResource(id = R.string.account_name),
                            summary = displayName,
                        ) {
                            launch {
                                val cookies = EhCookieStore.getIdentityCookies()
                                awaitConfirmationOrCancel(
                                    confirmText = R.string.settings_eh_sign_out,
                                    dismissText = R.string.settings_eh_clear_igneous,
                                    showCancelButton = cookies.last().second != null,
                                    onCancelButtonClick = { EhCookieStore.clearIgneous() },
                                    secure = true,
                                ) {
                                    Column {
                                        val warning = stringResource(id = R.string.settings_eh_identity_cookies_signed)
                                        val state = rememberTextFieldState(cookies.joinToString("\n") { (k, v) -> "$k: $v" })
                                        Text(text = AnnotatedString.fromHtml(warning))
                                        Spacer(modifier = Modifier.size(dimensionResource(id = com.hippo.ehviewer.R.dimen.keyline_margin)))
                                        TextField(
                                            state = state,
                                            readOnly = true,
                                            trailingIcon = {
                                                IconButton(
                                                    onClick = {
                                                        copyTextToClipboard(state.text, true)
                                                        // Avoid double notify user since system have done that on Tiramisu above
                                                        if (!isAtLeastT) launchSnackBar(copiedToClipboard)
                                                    },
                                                ) {
                                                    Icon(imageVector = MiuixIcons.Copy, contentDescription = null)
                                                }
                                            },
                                        )
                                    }
                                }
                                EhUtils.signOut()
                            }
                        }
                        val gallerySite = Settings.gallerySite.asMutableState()
                        SimpleMenuPreferenceInt(
                            title = stringResource(id = R.string.settings_eh_gallery_site),
                            entry = com.hippo.ehviewer.R.array.gallery_site_entries,
                            entryValueRes = com.hippo.ehviewer.R.array.gallery_site_entry_values,
                            state = gallerySite,
                        )
                        Preference(
                            title = stringResource(id = R.string.settings_u_config),
                            summary = stringResource(id = R.string.settings_u_config_summary),
                        ) { navigate(UConfigScreenDestination) }
                        Preference(
                            title = stringResource(id = R.string.settings_my_tags),
                            summary = stringResource(id = R.string.settings_my_tags_summary),
                        ) { navigate(MyTagsScreenDestination) }
                    }
                }

                SmallTitle(text = stringResource(id = R.string.settings_eh_list_mode))
                Card(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    var defaultFavSlot by Settings.defaultFavSlot.asMutableState()
                    val disabled = stringResource(id = R.string.disabled_nav)
                    val localFav = stringResource(id = R.string.local_favorites)
                    val summary = when (defaultFavSlot) {
                        -1 -> localFav
                        in 0..9 -> Settings.favCat[defaultFavSlot]
                        else -> stringResource(id = R.string.default_favorites_warning)
                    }
                    Preference(
                        title = stringResource(id = R.string.default_favorites_collection),
                        summary = summary,
                    ) {
                        launch {
                            val items = buildList {
                                add(disabled)
                                add(localFav)
                                if (hasSignedIn) {
                                    addAll(Settings.favCat)
                                }
                            }
                            defaultFavSlot = awaitSelectItem(
                                items = items,
                                title = R.string.default_favorites_collection,
                                selected = defaultFavSlot + 2,
                            ) - 2
                        }
                    }
                    SimpleMenuPreferenceInt(
                        title = stringResource(id = R.string.settings_eh_launch_page),
                        entry = com.hippo.ehviewer.R.array.launch_page_entries,
                        entryValueRes = com.hippo.ehviewer.R.array.launch_page_entry_values,
                        state = Settings.launchPage.asMutableState(),
                    )
                    val listMode = Settings.listMode.asMutableState()
                    SimpleMenuPreferenceInt(
                        title = stringResource(id = R.string.settings_eh_list_mode),
                        entry = com.hippo.ehviewer.R.array.list_mode_entries,
                        entryValueRes = com.hippo.ehviewer.R.array.list_mode_entry_values,
                        state = listMode,
                    )
                    AnimatedVisibility(visible = listMode.value == 0) {
                        Column {
                            IntSliderPreference(
                                maxValue = 60,
                                minValue = 20,
                                step = 7,
                                title = stringResource(id = R.string.list_tile_thumb_size),
                                state = Settings.listThumbSize.asMutableState(),
                            )
                            SimpleMenuPreferenceInt(
                                title = stringResource(id = R.string.settings_eh_detail_size),
                                entry = com.hippo.ehviewer.R.array.detail_size_entries,
                                entryValueRes = com.hippo.ehviewer.R.array.detail_size_entry_values,
                                state = Settings.detailSize.asMutableState(),
                            )
                        }
                    }
                    IntSliderPreference(
                        maxValue = 10,
                        minValue = 1,
                        title = stringResource(id = R.string.settings_eh_thumb_columns),
                        state = Settings.thumbColumns.asMutableState(),
                    )
                    SwitchPreference(
                        title = stringResource(id = R.string.settings_eh_show_gallery_pages),
                        summary = stringResource(id = R.string.settings_eh_show_gallery_pages_summary),
                        state = Settings.showGalleryPages.asMutableState(),
                    )
                    SwitchPreference(
                        title = stringResource(id = R.string.settings_eh_show_reading_progress),
                        summary = stringResource(id = R.string.settings_eh_show_reading_progress_summary),
                        state = Settings.showReadingProgress.asMutableState(),
                    )
                    SwitchPreference(
                        title = stringResource(id = R.string.settings_eh_show_vote_status),
                        state = Settings.showVoteStatus.asMutableState(),
                    )
                }

                SmallTitle(text = stringResource(id = R.string.dark_theme))
                Card(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    SimpleMenuPreferenceInt(
                        title = stringResource(id = R.string.dark_theme),
                        entry = com.hippo.ehviewer.R.array.night_mode_entries,
                        entryValueRes = com.hippo.ehviewer.R.array.night_mode_values,
                        state = Settings.theme.asMutableState(),
                    )
                    SwitchPreference(
                        title = stringResource(id = R.string.black_dark_theme),
                        state = Settings.blackDarkTheme.asMutableState(),
                    )
                    SwitchPreference(
                        title = stringResource(id = R.string.harmonize_category_color),
                        state = Settings.harmonizeCategoryColor.asMutableState(),
                    )
                }

                SmallTitle(text = stringResource(id = R.string.settings_eh_show_gallery_comments))
                Card(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    val showComments = Settings.showComments.asMutableState()
                    SwitchPreference(
                        title = stringResource(id = R.string.settings_eh_show_gallery_comments),
                        summary = stringResource(id = R.string.settings_eh_show_gallery_comments_summary),
                        state = showComments,
                    )
                    AnimatedVisibility(visible = showComments.value) {
                        IntSliderPreference(
                            maxValue = 100,
                            minValue = -101,
                            title = stringResource(id = R.string.settings_eh_show_gallery_comment_threshold),
                            summary = stringResource(id = R.string.settings_eh_show_gallery_comment_threshold_summary),
                            state = Settings.commentThreshold.asMutableState(),
                        )
                    }
                    if (hasSignedIn) {
                        SwitchPreference(
                            title = stringResource(id = R.string.settings_eh_show_jpn_title),
                            summary = stringResource(id = R.string.settings_eh_show_jpn_title_summary),
                            state = Settings.showJpnTitle.asMutableState(),
                        )
                        val reqNews = Settings.requestNews.asMutableState()
                        SwitchPreference(
                            title = stringResource(id = R.string.settings_eh_request_news),
                            state = reqNews,
                        )
                        AnimatedVisibility(visible = reqNews.value) {
                            val pickerTitle = stringResource(id = R.string.settings_eh_request_news_timepicker)
                            Preference(title = pickerTitle) {
                                launch {
                                    val time = LocalTime.fromSecondOfDay(Settings.requestNewsTime)
                                    val (hour, minute) = awaitSelectTime(pickerTitle, time.hour, time.minute)
                                    Settings.requestNewsTime = LocalTime(hour, minute).toSecondOfDay()
                                }
                            }
                        }
                        SwitchPreference(
                            title = stringResource(id = R.string.settings_eh_hide_hv_events),
                            state = Settings.hideHvEvents.asMutableState(),
                        )
                    }
                }

                SmallTitle(text = stringResource(id = R.string.settings_eh_filter))
                Card(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    if (EhTagDatabase.translatable) {
                        SwitchPreference(
                            title = stringResource(id = R.string.settings_eh_show_tag_translations),
                            summary = stringResource(id = R.string.settings_eh_show_tag_translations_summary),
                            state = Settings.showTagTranslations.asMutableState(),
                        )
                        UrlPreference(
                            title = stringResource(id = R.string.settings_eh_tag_translations_source),
                            url = stringResource(id = R.string.settings_eh_tag_translations_source_url),
                        )
                    }
                    Preference(
                        title = stringResource(id = R.string.settings_eh_filter),
                        summary = stringResource(id = R.string.settings_eh_filter_summary),
                    ) { navigate(FilterScreenDestination) }
                    SwitchPreference(
                        title = stringResource(id = R.string.settings_eh_metered_network_warning),
                        state = Settings.meteredNetworkWarning.asMutableState(),
                    )
                }
            }
        }
    }
}
