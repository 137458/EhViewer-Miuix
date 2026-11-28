package com.hippo.ehviewer.ui.reader

import eu.kanade.tachiyomi.ui.reader.setting.TappingInvertMode
import org.junit.Assert.assertEquals
import org.junit.Test

class TappingInvertModePreferenceTest {
    @Test
    fun `maps known preference values`() {
        assertEquals(TappingInvertMode.NONE, tappingInvertModeFromPreference(0))
        assertEquals(TappingInvertMode.HORIZONTAL, tappingInvertModeFromPreference(1))
        assertEquals(TappingInvertMode.VERTICAL, tappingInvertModeFromPreference(2))
        assertEquals(TappingInvertMode.BOTH, tappingInvertModeFromPreference(3))
    }

    @Test
    fun `maps unknown preference values to none`() {
        assertEquals(TappingInvertMode.NONE, tappingInvertModeFromPreference(4))
        assertEquals(TappingInvertMode.NONE, tappingInvertModeFromPreference(-1))
    }
}
