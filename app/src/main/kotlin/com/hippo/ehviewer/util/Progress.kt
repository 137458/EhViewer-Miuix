package com.hippo.ehviewer.util

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ehviewer.core.i18n.R
import com.hippo.ehviewer.ui.tools.DialogState
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun ProgressDialog() {
    WindowDialog(
        show = true,
        onDismissRequest = { },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
        ) {
            InfiniteProgressIndicator(modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(id = R.string.please_wait),
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }
    }
}

context(state: DialogState)
suspend fun <R> bgWork(work: suspend () -> R) = state.mutex.mutate {
    try {
        suspendCoroutineUninterceptedOrReturn { cont ->
            work.startCoroutineUninterceptedOrReturn(cont).also { r ->
                if (r == COROUTINE_SUSPENDED) state.value = { ProgressDialog() }
            }
        }
    } finally {
        state.dismiss()
    }
}
