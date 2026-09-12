package com.hippo.ehviewer.util

import org.junit.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebInjectionTest {

    @Test
    fun `viewport meta script contains mobile scale and viewport declaration`() {
        val script = WebInjectionHelper.buildViewportScript()
        assertContains(script, "name = 'viewport'")
        assertContains(script, "width=device-width")
        assertContains(script, "initial-scale=1.0")
    }

    @Test
    fun `responsive css script styles tables and stuffbox correctly`() {
        val cssScript = WebInjectionHelper.buildResponsiveCssScript()
        assertContains(cssScript, ".stuffbox")
        assertContains(cssScript, "max-width: 100%")
        assertContains(cssScript, "overflow-x: auto")
    }

    @Test
    fun `uconfig apply script targets apply button safely`() {
        val applyScript = WebInjectionHelper.APPLY_JS
        assertContains(applyScript, "document.getElementById(\"apply\")")
        assertContains(applyScript, "click()")
    }
}
