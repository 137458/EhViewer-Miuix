package com.hippo.ehviewer.ui.settings

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import com.ehviewer.core.i18n.R
import com.ehviewer.core.ui.component.BlurredBar
import com.ehviewer.core.ui.component.blurBackdropSource
import com.ehviewer.core.ui.component.rememberBlurBackdrop
import com.ehviewer.core.ui.util.readableWidth
import com.ehviewer.core.util.launch
import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.destinations.LicenseScreenDestination
import com.hippo.ehviewer.ui.destinations.UpdateScreenDestination
import com.hippo.ehviewer.ui.main.NavigationIcon
import com.hippo.ehviewer.ui.tools.awaitConfirmationOrCancel
import com.hippo.ehviewer.util.AppConfig
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import moe.tarsin.navigate
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val REPO_URL = "https://github.com/${BuildConfig.REPO_NAME}"
private const val RELEASE_URL = "$REPO_URL/releases"

@Composable
@Stable
private fun versionCode() = "${BuildConfig.VERSION_NAME} (${BuildConfig.COMMIT_SHA})\n" + stringResource(R.string.settings_about_commit_time, AppConfig.commitTime)

@Composable
@Stable
private fun author() = AnnotatedString.fromHtml(stringResource(R.string.settings_about_author_summary).replace('$', '@'))

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.AboutScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val scrollBehavior = MiuixScrollBehavior()
    val colorScheme = MiuixTheme.colorScheme
    fun showDisclaimer() = launch {
        awaitConfirmationOrCancel(
            title = R.string.settings_about_disclaimer,
            showCancelButton = false,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(text = stringResource(id = R.string.settings_about_disclaimer_content))
            }
        }
    }

    val backdrop = rememberBlurBackdrop()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
            ) {
                TopAppBar(
                    title = stringResource(id = R.string.settings_about),
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
                    .readableWidth()
                    .fillMaxWidth()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues),
            ) {
                SmallTitle(text = stringResource(id = R.string.settings_about))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Preference(
                        title = stringResource(id = R.string.settings_about_disclaimer),
                        summary = stringResource(id = R.string.settings_about_declaration_summary),
                        onClick = ::showDisclaimer,
                    )
                    HtmlPreference(
                        title = stringResource(id = R.string.settings_about_author),
                        summary = author(),
                    )
                    UrlPreference(
                        title = stringResource(id = R.string.settings_about_latest_release),
                        url = RELEASE_URL,
                    )
                    UrlPreference(
                        title = stringResource(id = R.string.settings_about_source),
                        url = REPO_URL,
                    )
                    ArrowPreference(
                        title = stringResource(id = R.string.license),
                        onClick = { navigate(LicenseScreenDestination) },
                    )
                    Preference(
                        title = stringResource(id = R.string.settings_about_version),
                        summary = versionCode(),
                    )
                }

                SmallTitle(text = stringResource(id = R.string.settings_about_check_for_updates))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    ArrowPreference(
                        title = stringResource(id = R.string.settings_about_check_for_updates),
                        summary = stringResource(id = R.string.update_pref_manual_check_idle),
                        onClick = { navigate(UpdateScreenDestination) },
                    )
                }
            }
        }
    }
}
