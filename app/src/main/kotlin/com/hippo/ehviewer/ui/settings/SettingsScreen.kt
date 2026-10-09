package com.hippo.ehviewer.ui.settings

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ehviewer.core.i18n.R
import com.ehviewer.core.ui.component.BlurredBar
import com.ehviewer.core.ui.component.blurBackdropSource
import com.ehviewer.core.ui.component.rememberBlurBackdrop
import com.ehviewer.core.ui.icons.EhIcons
import com.ehviewer.core.ui.icons.filled.SadPanda
import com.ehviewer.core.ui.util.LocalBottomBarContentPadding
import com.ehviewer.core.ui.util.readableWidth
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.ui.MainNavItems
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.destinations.AboutScreenDestination
import com.hippo.ehviewer.ui.destinations.AdvancedScreenDestination
import com.hippo.ehviewer.ui.destinations.BottomNavItemsScreenDestination
import com.hippo.ehviewer.ui.destinations.DownloadScreenDestination
import com.hippo.ehviewer.ui.destinations.EhScreenDestination
import com.hippo.ehviewer.ui.destinations.PrivacyScreenDestination
import com.hippo.ehviewer.ui.direction
import com.hippo.ehviewer.ui.main.NavigationIcon
import com.hippo.ehviewer.ui.titleRes
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.SettingsScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val scrollBehavior = MiuixScrollBehavior()
    val colorScheme = MiuixTheme.colorScheme
    val backdrop = rememberBlurBackdrop()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
            ) {
                TopAppBar(
                    title = stringResource(id = R.string.settings),
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
                    .padding(paddingValues)
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .verticalScroll(rememberScrollState()),
            ) {
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
                        icon = MiuixIcons.Download,
                        title = R.string.settings_download,
                        childRoute = DownloadScreenDestination,
                        navigator = navigator,
                    )
                    PreferenceHeader(
                        icon = MiuixIcons.Lock,
                        title = R.string.settings_privacy,
                        childRoute = PrivacyScreenDestination,
                        navigator = navigator,
                    )
                    PreferenceHeader(
                        icon = MiuixIcons.Tune,
                        title = R.string.settings_advanced,
                        childRoute = AdvancedScreenDestination,
                        navigator = navigator,
                    )
                    PreferenceHeader(
                        icon = MiuixIcons.Info,
                        title = R.string.settings_about,
                        childRoute = AboutScreenDestination,
                        navigator = navigator,
                    )
                    PreferenceHeader(
                        icon = MiuixIcons.More,
                        title = R.string.desktop_nav_items_title,
                        childRoute = BottomNavItemsScreenDestination,
                        navigator = navigator,
                    )
                }
                // 底栏未显示的页面在此保留入口，避免自定义底栏后页面不可达
                val rawNavConfig by Settings.bottomNavItems.collectAsState()
                val hiddenNavItems = remember(rawNavConfig) {
                    val config = MainNavItems.decode(rawNavConfig)
                    config.order.filter { it in config.hidden }
                }
                if (hiddenNavItems.isNotEmpty()) {
                    SmallTitle(
                        text = stringResource(id = R.string.settings_hidden_pages),
                        modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 4.dp),
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        hiddenNavItems.forEach { item ->
                            ArrowPreference(
                                title = stringResource(id = item.titleRes),
                                startAction = {
                                    Icon(
                                        imageVector = item.icon(),
                                        contentDescription = null,
                                        tint = MiuixTheme.colorScheme.primary,
                                    )
                                },
                                onClick = { navigator.navigate(item.direction) },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(LocalBottomBarContentPadding.current))
            }
        }
    }
}
