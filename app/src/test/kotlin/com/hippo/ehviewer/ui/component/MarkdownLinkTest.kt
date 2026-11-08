package com.hippo.ehviewer.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MarkdownLinkTest {
    @Test
    fun `keeps absolute http links`() {
        assertEquals("https://example.com/a", linkifiableUrl("https://example.com/a"))
        assertEquals("HTTP://example.com", linkifiableUrl("HTTP://example.com"))
    }

    @Test
    fun `drops relative and unknown schemes`() {
        assertNull(linkifiableUrl("docs/CHANGELOG.md"))
        assertNull(linkifiableUrl("/releases/tag/1.2.3"))
        assertNull(linkifiableUrl("javascript:alert(1)"))
        assertNull(linkifiableUrl("mailto:a@b.c"))
    }
}
