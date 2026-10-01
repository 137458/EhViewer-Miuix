package com.ehviewer.desktop

sealed class DesktopImageLoadState {
    data object Loading : DesktopImageLoadState()
    data object Success : DesktopImageLoadState()
    data class Error(val reason: String = "Failed to load") : DesktopImageLoadState()
}

class DesktopImageStateController(
    initialState: DesktopImageLoadState = DesktopImageLoadState.Loading,
) {
    var state: DesktopImageLoadState = initialState
        private set

    var retryCount: Int = 0
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
