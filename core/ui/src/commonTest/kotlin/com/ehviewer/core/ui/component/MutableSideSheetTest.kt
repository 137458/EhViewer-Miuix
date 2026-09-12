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
}
