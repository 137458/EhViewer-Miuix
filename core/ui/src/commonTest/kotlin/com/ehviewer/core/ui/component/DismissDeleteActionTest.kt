package com.ehviewer.core.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsModifier
import androidx.compose.ui.semantics.getOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DismissDeleteActionTest {
    @Test
    fun dismissDeleteActionConfiguresAccessibilityAction() {
        var deleted = false
        val modifier = Modifier.dismissDeleteAction("Delete") {
            deleted = true
        }

        var config: SemanticsConfiguration? = null
        modifier.foldIn(Unit) { _, element ->
            if (element is SemanticsModifier) {
                config = element.semanticsConfiguration
            }
        }

        val checkedConfig = requireNotNull(config)
        val actions = checkedConfig.getOrNull(SemanticsActions.CustomActions)
        requireNotNull(actions)
        assertEquals(1, actions.size)
        assertEquals("Delete", actions[0].label)
        val result = actions[0].action()
        assertTrue(result)
        assertTrue(deleted)
    }
}
