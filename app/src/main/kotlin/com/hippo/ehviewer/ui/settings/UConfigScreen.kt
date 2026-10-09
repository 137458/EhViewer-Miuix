package com.hippo.ehviewer.ui.settings

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ehviewer.core.i18n.R
import com.ehviewer.core.network.EhCookieStore
import com.ehviewer.core.util.launch
import com.google.accompanist.web.LoadingState
import com.google.accompanist.web.rememberWebViewNavigator
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
import kotlinx.coroutines.delay
import moe.tarsin.snackbar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Ok

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.UConfigScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val url = EhUrl.getUConfigUrl()
    val wvNavigator = rememberWebViewNavigator()
    var isApplying by rememberSaveable { mutableStateOf(false) }
    val state = rememberWebViewState(url = url)

    LaunchedEffect(state.loadingState) {
        if (state.loadingState is LoadingState.Finished) {
            wvNavigator.loadUrl(WebInjectionHelper.buildViewportScript())
            wvNavigator.loadUrl(WebInjectionHelper.buildResponsiveCssScript())
            if (isApplying) {
                navigator.popBackStack()
            }
        }
    }
    LaunchedEffect(isApplying) {
        if (isApplying) {
            delay(3000)
            navigator.popBackStack()
        }
    }

    WebViewScaffold(
        title = stringResource(id = R.string.u_config),
        state = state,
        navigator = wvNavigator,
        onCreated = { it.setDefaultSettings() },
        navigationIcon = { NavigationIcon() },
    ) {
        if (isApplying) {
            InfiniteProgressIndicator(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(24.dp),
            )
        } else {
            IconButton(
                onClick = {
                    isApplying = true
                    wvNavigator.loadUrl(WebInjectionHelper.APPLY_JS)
                },
            ) {
                Icon(
                    imageVector = MiuixIcons.Ok,
                    contentDescription = stringResource(id = android.R.string.ok),
                )
            }
        }
    }

    val applyTip = stringResource(id = R.string.apply_tip)
    DisposableEffect(Unit) {
        launch { snackbar(applyTip) }
        onDispose {
            EhCookieStore.flush()
        }
    }
}
