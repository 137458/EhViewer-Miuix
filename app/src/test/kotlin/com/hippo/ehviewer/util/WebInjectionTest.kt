package com.hippo.ehviewer.util

import org.junit.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebInjectionTest {

    @Test
    fun `viewport script adds viewport metadata only when it is missing`() {
        val script = WebInjectionHelper.buildViewportScript()

        assertContains(script, "document.querySelector('meta[name=\"viewport\"]')")
        assertContains(script, "meta.name = 'viewport'")
        assertContains(script, "meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=3.0'")
        assertContains(script, "head.appendChild(meta)")
        assertContains(script, "if (!head)")
    }

    @Test
    fun `responsive css script preserves css syntax and creates a style node`() {
        val cssScript = WebInjectionHelper.buildResponsiveCssScript()

        assertContains(cssScript, "var style = document.createElement('style')")
        assertContains(cssScript, "style.textContent = '")
        assertContains(cssScript, ".stuffbox { max-width: 100% !important;")
        assertContains(cssScript, "overflow-x: auto !important")
        assertContains(cssScript, "head.appendChild(style)")
    }

    @Test
    fun `css content is escaped before it is embedded in a javascript string`() {
        val css = "body::before { content: 'line 1\nline 2'; }"
        val script = WebInjectionHelper.buildCssInjectionScript(css)

        assertContains(script, """content: \'line 1\nline 2\'""")
        assertFalse(script.contains("style.innerHTML ="))
    }

    @Test
    fun `css content escapes characters that can terminate a javascript string`() {
        val css = "body::before { content: 'C:\\path\rline\u2028next\u2029'; }"
        val script = WebInjectionHelper.buildCssInjectionScript(css)

        assertContains(script, """C:\\path\rline\u2028next\u2029""")
        assertFalse(script.contains('\r'))
        assertFalse(script.contains('\u2028'))
        assertFalse(script.contains('\u2029'))
    }

    @Test
    fun `uconfig apply script checks target and child before clicking`() {
        val applyScript = WebInjectionHelper.APPLY_JS

        assertContains(applyScript, "document.getElementById(\"apply\")")
        assertContains(applyScript, "apply && apply.children.length > 0")
        assertContains(applyScript, "apply.children[0].click()")
    }
}
