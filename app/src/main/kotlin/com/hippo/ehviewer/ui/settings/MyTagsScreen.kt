package com.hippo.ehviewer.ui.settings

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.ehviewer.core.i18n.R
import com.ehviewer.core.ui.component.BlurredBar
import com.ehviewer.core.ui.component.blurBackdropSource
import com.ehviewer.core.ui.component.rememberBlurBackdrop
import com.google.accompanist.web.LoadingState
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import com.hippo.ehviewer.client.EhUrl
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
fun AnimatedVisibilityScope.MyTagsScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val url = EhUrl.myTagsUrl
    val state = rememberWebViewState(url = url)

    LaunchedEffect(state.loadingState) {
        if (state.loadingState is LoadingState.Finished) {
            state.webView?.evaluateJavascript(WebInjectionHelper.VIEWPORT_META_INJECTION_SCRIPT, null)
            state.webView?.evaluateJavascript(
                WebInjectionHelper.buildCssInjectionScript(WebInjectionHelper.RESPONSIVE_TABLE_CSS),
                null,
            )
        }
    }

    val backdrop = rememberBlurBackdrop()

    Scaffold(
        topBar = {
            BlurredBar(
                backdrop = backdrop,
            ) {
                TopAppBar(
                    title = stringResource(id = R.string.my_tags),
                    navigationIcon = { NavigationIcon() },
                    color = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface,
                    actions = {
                        if (state.isLoading) {
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
                .background(MiuixTheme.colorScheme.background)
                .blurBackdropSource(backdrop),
        ) {
            WebView(
                state = state,
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                onCreated = {
                    it.setDefaultSettings()
                    it.evaluateJavascript(WebInjectionHelper.VIEWPORT_META_INJECTION_SCRIPT, null)
                    it.evaluateJavascript(
                        WebInjectionHelper.buildCssInjectionScript(WebInjectionHelper.RESPONSIVE_TABLE_CSS),
                        null,
                    )
                },
            )
        }
    }
}
