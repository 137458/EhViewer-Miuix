package moe.tarsin.coroutines

import com.ehviewer.core.util.launch
import com.hippo.ehviewer.util.displayString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import moe.tarsin.snackbar
import top.yukonga.miuix.kmp.basic.SnackbarHostState

inline fun <T, reified E : Throwable> Result<T>.except() = onFailure { if (it is E) throw it }

inline fun <R> runSuspendCatching(block: () -> R) = runCatching(block).except<R, CancellationException>()

inline fun <T, R> T.runSuspendCatching(block: T.() -> R) = runCatching(block).except<R, CancellationException>()

context(_: SnackbarHostState, _: CoroutineScope)
inline fun <R> runSwallowingWithUI(block: () -> R) = runSuspendCatching(block).onFailure { e -> launch { snackbar(e.displayString()) } }
