package com.ehviewer.desktop

import androidx.compose.runtime.State
import androidx.compose.runtime.snapshots.Snapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopImageStateControllerTest {

    @Test
    fun initialState_defaultsToLoading() {
        val controller = DesktopImageStateController()
        assertEquals(DesktopImageLoadState.Loading, controller.state)
        assertEquals(0, controller.retryCount)
        assertFalse(controller.canRetry)
    }

    @Test
    fun onSuccess_transitionsToSuccess() {
        val controller = DesktopImageStateController()
        controller.onSuccess()
        assertEquals(DesktopImageLoadState.Success, controller.state)
        assertFalse(controller.canRetry)
    }

    @Test
    fun onError_transitionsToErrorWithReason() {
        val controller = DesktopImageStateController()
        controller.onError("HTTP 404")
        val state = controller.state
        assertTrue(state is DesktopImageLoadState.Error)
        assertEquals("HTTP 404", state.reason)
        assertTrue(controller.canRetry)

        // 三角验证：空 reason 时默认提供回退
        controller.onError(null)
        val stateNull = controller.state
        assertTrue(stateNull is DesktopImageLoadState.Error)
        assertEquals("Failed to load", stateNull.reason)
    }

    @Test
    fun retry_whenInErrorState_incrementsCountAndTransitionsToLoading() {
        val controller = DesktopImageStateController()
        controller.onError("Timeout")
        assertTrue(controller.canRetry)

        val firstRetry = controller.retry()
        assertTrue(firstRetry)
        assertEquals(1, controller.retryCount)
        assertEquals(DesktopImageLoadState.Loading, controller.state)
        assertFalse(controller.canRetry)

        // 再次报错后二次重试
        controller.onError("Connection reset")
        val secondRetry = controller.retry()
        assertTrue(secondRetry)
        assertEquals(2, controller.retryCount)
        assertEquals(DesktopImageLoadState.Loading, controller.state)
    }

    @Test
    fun retry_whenNotInErrorState_doesNotChangeStateOrCount() {
        val controller = DesktopImageStateController()
        // 当前处于 Loading
        assertFalse(controller.retry())
        assertEquals(0, controller.retryCount)
        assertEquals(DesktopImageLoadState.Loading, controller.state)

        // 处于 Success
        controller.onSuccess()
        assertFalse(controller.retry())
        assertEquals(0, controller.retryCount)
        assertEquals(DesktopImageLoadState.Success, controller.state)
    }

    @Test
    fun mutationsArePublishedThroughComposeSnapshots() {
        // H-36 回归：state/retryCount 必须是 Compose Snapshot State。
        // 判别法：Snapshot State 的写入会被记录进 apply observer 的变更集，普通 var 永远不会被记录。
        val controller = DesktopImageStateController()
        val changeSets = mutableListOf<Set<Any>>()
        val handle = Snapshot.registerApplyObserver { objects, _ -> changeSets += objects }
        try {
            controller.onError("boom")
            Snapshot.sendApplyNotifications()
            assertTrue(changeSets.isNotEmpty(), "onError 必须通知快照观察者（普通 var 不会）")
            assertTrue(
                changeSets.any { objects -> objects.any { it is State<*> } },
                "变更集里应包含 Snapshot State 本体",
            )
            assertEquals(DesktopImageLoadState.Error("boom"), controller.state)

            changeSets.clear()
            controller.retry()
            Snapshot.sendApplyNotifications()
            assertTrue(changeSets.isNotEmpty(), "retryCount 的变更也必须进入快照通知")
            assertEquals(1, controller.retryCount)
            assertEquals(DesktopImageLoadState.Loading, controller.state)
        } finally {
            handle.dispose()
        }
    }
}
