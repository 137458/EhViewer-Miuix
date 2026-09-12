package com.hippo.ehviewer.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton

@Composable
context(navigator: DestinationsNavigator)
fun NavigationIcon() = IconButton(onClick = { navigator.popBackStack() }) {
    Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
}
