package com.hippo.ehviewer.ui.main

import androidx.compose.runtime.Composable
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

@Composable
context(navigator: DestinationsNavigator)
fun NavigationIcon() = IconButton(onClick = { navigator.popBackStack() }) {
    Icon(imageVector = MiuixIcons.Back, contentDescription = null)
}
