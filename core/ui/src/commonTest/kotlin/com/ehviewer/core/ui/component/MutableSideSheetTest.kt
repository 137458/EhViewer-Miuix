package com.ehviewer.core.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MutableSideSheetTest {

    @Test
    fun `side sheet starts closed by default`() {
        val state = SideSheetState()

        assertEquals(SideSheetValue.Closed, state.currentValue)
        assertTrue(state.isClosed)
        assertFalse(state.isOpen)
    }

    @Test
    fun `side sheet reflects an explicitly open initial state`() {
        val state = SideSheetState(SideSheetValue.Open)

        assertEquals(SideSheetValue.Open, state.currentValue)
        assertTrue(state.isOpen)
        assertFalse(state.isClosed)
    }

    @Test
    fun `saver restores a saved-open sheet as closed`() {
        // 侧板是瞬态层：跨进程恢复 Open 会启动即弹面板并残留全屏模糊，恢复必须钳回 Closed
        val restored = requireNotNull(SideSheetState.Saver.restore(SideSheetValue.Open))

        assertEquals(SideSheetValue.Closed, restored.currentValue)
        assertTrue(restored.isClosed)
    }
}
