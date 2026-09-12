package com.hippo.ehviewer.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import com.ehviewer.core.i18n.R
import com.hippo.ehviewer.util.displayString
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun LoadStateIndicator(
    modifier: Modifier = Modifier,
    state: LoadState,
    retry: () -> Unit,
) = when (state) {
    is LoadState.Loading -> {
        LinearProgressIndicator(modifier = modifier)
    }
    is LoadState.Error -> {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = state.error.displayString())
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = retry) {
                Text(text = stringResource(id = R.string.action_retry))
            }
        }
    }
    is LoadState.NotLoading -> Unit
}
