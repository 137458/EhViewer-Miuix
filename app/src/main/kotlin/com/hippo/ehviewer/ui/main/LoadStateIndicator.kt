package com.hippo.ehviewer.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import com.ehviewer.core.i18n.R
import com.ehviewer.core.ui.component.ErrorTip
import com.hippo.ehviewer.util.displayString
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator

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
        ErrorTip(
            modifier = modifier,
            text = state.error.displayString(),
            retryText = stringResource(id = R.string.action_retry),
            onRetry = retry,
            compact = true,
        )
    }
    is LoadState.NotLoading -> Unit
}
