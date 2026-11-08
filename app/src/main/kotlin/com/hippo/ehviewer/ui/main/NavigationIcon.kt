package com.hippo.ehviewer.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ehviewer.core.i18n.R
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

@Composable
context(navigator: DestinationsNavigator)
fun NavigationIcon() = IconButton(onClick = { navigator.popBackStack() }) {
    Icon(
        imageVector = MiuixIcons.Back,
        contentDescription = stringResource(id = R.string.desktop_a11y_back),
    )
}
