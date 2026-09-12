package com.hippo.ehviewer.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ehviewer.core.i18n.R
import com.ehviewer.core.ui.component.BlurredBar
import com.ehviewer.core.util.launch
import com.hippo.ehviewer.EhApplication.Companion.searchDatabase
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.asMutableState
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.isAuthenticationSupported
import com.hippo.ehviewer.ui.main.NavigationIcon
import com.hippo.ehviewer.ui.tools.awaitConfirmationOrCancel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import moe.tarsin.snackbar
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.PrivacyScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val scrollBehavior = MiuixScrollBehavior()
    fun launchSnackbar(message: String) = launch { snackbar(message) }
    Scaffold(
        topBar = {
            BlurredBar {
                TopAppBar(
                    title = stringResource(id = R.string.settings_privacy),
                    navigationIcon = { NavigationIcon() },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxHeight()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues),
            ) {
                SmallTitle(text = stringResource(id = R.string.settings_privacy))
                Card(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    val security = Settings.security.asMutableState()
                    SwitchPreference(
                        title = stringResource(id = R.string.settings_privacy_require_unlock),
                        state = security,
                        enabled = isAuthenticationSupported(),
                    )
                    AnimatedVisibility(visible = security.value) {
                        val securityDelay = Settings.securityDelay.asMutableState()
                        val summary = if (securityDelay.value == 0) {
                            stringResource(id = R.string.settings_privacy_require_unlock_delay_summary_immediately)
                        } else {
                            stringResource(id = R.string.settings_privacy_require_unlock_delay_summary, securityDelay.value)
                        }
                        IntSliderPreference(
                            maxValue = 30,
                            title = stringResource(id = R.string.settings_privacy_require_unlock_delay),
                            summary = summary,
                            state = securityDelay,
                            enabled = isAuthenticationSupported(),
                        )
                    }
                    SwitchPreference(
                        title = stringResource(id = R.string.settings_privacy_secure),
                        summary = stringResource(id = R.string.settings_privacy_secure_summary),
                        state = Settings.enabledSecurity.asMutableState(),
                    )
                }

                SmallTitle(text = stringResource(id = R.string.clear_search_history))
                Card(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    val searchHistoryCleared = stringResource(id = R.string.search_history_cleared)
                    Preference(
                        title = stringResource(id = R.string.clear_search_history),
                        summary = stringResource(id = R.string.clear_search_history_summary),
                    ) {
                        launch {
                            awaitConfirmationOrCancel(
                                confirmText = R.string.clear_all,
                                title = R.string.clear_search_history_confirm,
                            )
                            searchDatabase.searchDao().clear()
                            launchSnackbar(searchHistoryCleared)
                        }
                    }
                }
            }
        }
    }
}
