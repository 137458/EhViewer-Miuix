package com.hippo.ehviewer.ui.component

import androidx.compose.ui.text.LinkAnnotation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownTextLinkTest {
    @Test
    fun `markdown link becomes a clickable url annotation`() {
        val annotated = buildAnnotatedContent("详见 [发布说明](https://example.invalid/release)")

        assertEquals("详见 发布说明", annotated.text)
        val links = annotated.getLinkAnnotations(0, annotated.length)
        assertEquals(1, links.size)
        val annotation = links.single().item
        assertTrue(annotation is LinkAnnotation.Url)
        assertEquals("https://example.invalid/release", (annotation as LinkAnnotation.Url).url)
    }

    @Test
    fun `unclosed markdown link stays literal`() {
        val raw = "见 [说明](https://example.invalid"

        assertEquals(raw, buildAnnotatedContent(raw).text)
    }

    @Test
    fun `nested emphasis inside link stays bold`() {
        val annotated = buildAnnotatedContent("[**重点**](https://example.invalid)")

        assertEquals("重点", annotated.text)
        assertTrue(annotated.getLinkAnnotations(0, annotated.length).isNotEmpty())
    }
}
