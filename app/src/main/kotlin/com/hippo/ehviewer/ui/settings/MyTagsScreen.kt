package com.hippo.ehviewer.ui.settings

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.ehviewer.core.i18n.R
import com.google.accompanist.web.LoadingState
import com.google.accompanist.web.rememberWebViewState
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.main.NavigationIcon
import com.hippo.ehviewer.ui.main.WebViewScaffold
import com.hippo.ehviewer.util.WebInjectionHelper
import com.hippo.ehviewer.util.setDefaultSettings
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator

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

    WebViewScaffold(
        title = stringResource(id = R.string.my_tags),
        state = state,
        onCreated = {
            it.setDefaultSettings()
            it.evaluateJavascript(WebInjectionHelper.VIEWPORT_META_INJECTION_SCRIPT, null)
            it.evaluateJavascript(
                WebInjectionHelper.buildCssInjectionScript(WebInjectionHelper.RESPONSIVE_TABLE_CSS),
                null,
            )
        },
        navigationIcon = { NavigationIcon() },
    ) {
        if (state.isLoading) {
            InfiniteProgressIndicator()
        }
    }
}
