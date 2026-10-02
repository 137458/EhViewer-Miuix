package com.ehviewer.desktop

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopSingleInstanceTest {
    @Test
    fun acquireBindsLoopbackPortAndRejectsSecondAcquire() {
        val port = 4_1527
        assertTrue(DesktopSingleInstance.tryAcquire(port))
        // 同端口第二次获取必须失败（已在运行）
        assertFalse(DesktopSingleInstance.tryAcquire(port))
        DesktopSingleInstance.release()
        // 释放后可重新获取
        assertTrue(DesktopSingleInstance.tryAcquire(port))
        DesktopSingleInstance.release()
    }

    @Test
    fun differentPortsAreIndependent() {
        assertTrue(DesktopSingleInstance.tryAcquire(4_1528))
        assertTrue(DesktopSingleInstance.tryAcquire(4_1529))
        DesktopSingleInstance.release()
        DesktopSingleInstance.release()
    }
}
