package com.hippo.ehviewer.ui.settings

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ehviewer.core.i18n.R
import com.ehviewer.core.ui.component.BlurredBar
import com.ehviewer.core.ui.icons.EhIcons
import com.ehviewer.core.ui.icons.filled.SadPanda
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.destinations.AboutScreenDestination
import com.hippo.ehviewer.ui.destinations.AdvancedScreenDestination
import com.hippo.ehviewer.ui.destinations.DownloadScreenDestination
import com.hippo.ehviewer.ui.destinations.EhScreenDestination
import com.hippo.ehviewer.ui.destinations.PrivacyScreenDestination
import com.hippo.ehviewer.ui.main.NavigationIcon
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.SettingsScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val scrollBehavior = MiuixScrollBehavior()
    val colorScheme = MiuixTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BlurredBar(
                backdrop = null,
                scrollBehavior = scrollBehavior,
            ) {
                TopAppBar(
                    title = stringResource(id = R.string.settings),
                    navigationIcon = { NavigationIcon() },
                    scrollBehavior = scrollBehavior,
                    color = colorScheme.surface,
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .verticalScroll(rememberScrollState()),
            ) {
                SmallTitle(text = stringResource(id = R.string.settings))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    PreferenceHeader(
                        icon = EhIcons.Default.SadPanda,
                        title = R.string.settings_eh,
                        childRoute = EhScreenDestination,
                        navigator = navigator,
                    )
                    PreferenceHeader(
                        icon = Icons.Default.Download,
                        title = R.string.settings_download,
                        childRoute = DownloadScreenDestination,
                        navigator = navigator,
                    )
                    PreferenceHeader(
                        icon = Icons.Default.Security,
                        title = R.string.settings_privacy,
                        childRoute = PrivacyScreenDestination,
                        navigator = navigator,
                    )
                    PreferenceHeader(
                        icon = Icons.Default.Adb,
                        title = R.string.settings_advanced,
                        childRoute = AdvancedScreenDestination,
                        navigator = navigator,
                    )
                    PreferenceHeader(
                        icon = Icons.Default.Info,
                        title = R.string.settings_about,
                        childRoute = AboutScreenDestination,
                        navigator = navigator,
                    )
                }
            }
        }
    }
}
