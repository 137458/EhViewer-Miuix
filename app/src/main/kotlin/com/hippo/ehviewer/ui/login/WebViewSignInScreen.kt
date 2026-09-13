package com.hippo.ehviewer.ui.login

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.ehviewer.core.i18n.R
import com.ehviewer.core.network.EhCookieStore
import com.ehviewer.core.ui.component.BlurredBar
import com.ehviewer.core.ui.component.blurBackdropSource
import com.ehviewer.core.ui.component.rememberBlurBackdrop
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.main.NavigationIcon
import com.hippo.ehviewer.util.WebInjectionHelper
import com.hippo.ehviewer.util.setDefaultSettings
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.WebViewSignInScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val state = rememberWebViewState(url = EhUrl.URL_SIGN_IN)
    var isHandlingLogin by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        snapshotFlow { !state.isLoading }.collect { hasFinished ->
            if (hasFinished && !isHandlingLogin) {
                state.webView?.evaluateJavascript(WebInjectionHelper.VIEWPORT_META_INJECTION_SCRIPT, null)
                if (EhCookieStore.isCloudflareBypassed()) {
                    Settings.desktopSite.value = false
                }
                if (EhCookieStore.hasSignedIn()) {
                    isHandlingLogin = true
                    EhCookieStore.flush()
                    postLogin().await()
                    navigator.popBackStack()
                }
            }
        }
    }
    val backdrop = rememberBlurBackdrop()

    Scaffold(
        topBar = {
            BlurredBar(
                backdrop = backdrop,
            ) {
                TopAppBar(
                    title = stringResource(id = R.string.sign_in),
                    navigationIcon = { NavigationIcon() },
                    color = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface,
                    actions = {
                        if (state.isLoading || isHandlingLogin) {
                            InfiniteProgressIndicator()
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blurBackdropSource(backdrop),
        ) {
            WebView(
                state = state,
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                onCreated = {
                    EhUtils.signOut()
                    it.setDefaultSettings()
                    it.evaluateJavascript(WebInjectionHelper.VIEWPORT_META_INJECTION_SCRIPT, null)
                },
            )
        }
    }
}
