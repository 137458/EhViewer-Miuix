package com.hippo.ehviewer.ui.settings

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ehviewer.core.i18n.R
import com.ehviewer.core.ui.component.BlurredBar
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.main.NavigationIcon
import com.hippo.ehviewer.ui.openBrowser
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.LicenseScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val scrollBehavior = MiuixScrollBehavior()
    Scaffold(
        topBar = {
            BlurredBar {
                TopAppBar(
                    title = stringResource(id = R.string.license),
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
        ) {
            val libraries by produceLibraries(com.hippo.ehviewer.R.raw.aboutlibraries)
            LibrariesContainer(
                libraries = libraries,
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = paddingValues,
                padding = LibraryDefaults.libraryPadding(licensePadding = LibraryDefaults.chipPadding(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp))),
                textStyles = LibraryDefaults.libraryTextStyles(licensesTextStyle = MaterialTheme.typography.labelSmall),
                onLibraryClick = { library ->
                    library.website?.let { openBrowser(it) }
                },
            )
        }
    }
}
