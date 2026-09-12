package com.hippo.ehviewer.ui.settings

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ehviewer.core.i18n.R
import com.ehviewer.core.network.EhCookieStore
import com.ehviewer.core.ui.component.BlurredBar
import com.ehviewer.core.util.launch
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewNavigator
import com.google.accompanist.web.rememberWebViewState
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.main.NavigationIcon
import com.hippo.ehviewer.util.setDefaultSettings
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import moe.tarsin.snackbar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val APPLY_JS = "javascript:(function(){var apply = document.getElementById(\"apply\").children[0];apply.click();})();"

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.UConfigScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val url = EhUrl.getUConfigUrl()
    val wvNavigator = rememberWebViewNavigator()
    Scaffold(
        topBar = {
            BlurredBar {
                TopAppBar(
                    title = stringResource(id = R.string.u_config),
                    navigationIcon = { NavigationIcon() },
                    actions = {
                        IconButton(
                            onClick = {
                                wvNavigator.loadUrl(APPLY_JS)
                                navigator.popBackStack()
                            },
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background),
        ) {
            val state = rememberWebViewState(url = url)
            WebView(
                state = state,
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                navigator = wvNavigator,
                onCreated = { it.setDefaultSettings() },
            )
        }
        val applyTip = stringResource(id = R.string.apply_tip)
        DisposableEffect(Unit) {
            launch { snackbar(applyTip) }
            onDispose {
                EhCookieStore.flush()
            }
        }
    }
}
