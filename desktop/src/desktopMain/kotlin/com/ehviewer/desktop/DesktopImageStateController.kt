package com.ehviewer.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

sealed class DesktopImageLoadState {
    data object Loading : DesktopImageLoadState()
    data object Success : DesktopImageLoadState()
    data class Error(val reason: String = "Failed to load") : DesktopImageLoadState()
}

class DesktopImageStateController(
    initialState: DesktopImageLoadState = DesktopImageLoadState.Loading,
) {
    // Compose Snapshot State：加载器在后台线程写入后由快照系统通知读端重组；
    // 普通 var 不参与快照，后台写入既不会触发重组，也缺少跨线程可见性保证
    var state: DesktopImageLoadState by mutableStateOf(initialState)
        private set

    var retryCount: Int by mutableIntStateOf(0)
        private set

    val canRetry: Boolean get() = state is DesktopImageLoadState.Error

    fun onLoading() {
        state = DesktopImageLoadState.Loading
    }

    fun onSuccess() {
        state = DesktopImageLoadState.Success
    }

    fun onError(reason: String? = null) {
        state = DesktopImageLoadState.Error(reason ?: "Failed to load")
    }

    fun retry(): Boolean = if (state is DesktopImageLoadState.Error) {
        retryCount++
        state = DesktopImageLoadState.Loading
        true
    } else {
        false
    }
}
