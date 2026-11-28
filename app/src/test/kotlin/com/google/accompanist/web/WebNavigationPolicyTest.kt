package com.google.accompanist.web

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebNavigationPolicyTest {
    @Test
    fun ehHttpsHostsLoadInWebView() {
        assertTrue(WebNavigationPolicy.shouldLoadInWebView("https://e-hentai.org/"))
        assertTrue(WebNavigationPolicy.shouldLoadInWebView("https://forums.e-hentai.org/index.php"))
        assertTrue(WebNavigationPolicy.shouldLoadInWebView("https://exhentai.org/g/1/token/"))
        assertTrue(WebNavigationPolicy.shouldLoadInWebView("https://challenges.cloudflare.com/turnstile"))
        assertTrue(WebNavigationPolicy.shouldLoadInWebView("https://ehgt.org/t/1.jpg"))
    }

    @Test
    fun plainHttpIsBlocked() {
        assertFalse(WebNavigationPolicy.shouldLoadInWebView("http://e-hentai.org/"))
    }

    @Test
    fun localAndCustomSchemesAreBlocked() {
        assertFalse(WebNavigationPolicy.shouldLoadInWebView("file:///data/user/0/com.hippo.ehviewer/files/eh.db"))
        assertFalse(WebNavigationPolicy.shouldLoadInWebView("content://com.android.providers.media.documents/document/1"))
        assertFalse(WebNavigationPolicy.shouldLoadInWebView("intent://scan/#Intent;scheme=zxing;end"))
        assertFalse(WebNavigationPolicy.shouldLoadInWebView("javascript:alert(1)"))
        assertFalse(WebNavigationPolicy.shouldLoadInWebView("data:text/html,<h1>x</h1>"))
        assertFalse(WebNavigationPolicy.shouldLoadInWebView("about:blank"))
    }

    @Test
    fun foreignHostsAreBlocked() {
        assertFalse(WebNavigationPolicy.shouldLoadInWebView("https://example.com/"))
        assertFalse(WebNavigationPolicy.shouldLoadInWebView("https://evil-e-hentai.org/"))
        assertFalse(WebNavigationPolicy.shouldLoadInWebView("https://e-hentai.org.evil.com/"))
        assertFalse(WebNavigationPolicy.shouldLoadInWebView(null))
        assertFalse(WebNavigationPolicy.shouldLoadInWebView(""))
    }
}
