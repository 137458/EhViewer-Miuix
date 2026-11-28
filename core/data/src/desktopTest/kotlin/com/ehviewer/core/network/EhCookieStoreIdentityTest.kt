package com.ehviewer.core.network

import com.ehviewer.core.desktop.testing.clearIsolatedDataDir
import com.ehviewer.core.desktop.testing.newIsolatedDataDir
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// 身份 cookie（ipb_member_id / ipb_pass_hash / igneous）必须带 Secure：
// 明文 HTTP 请求不得携带登录凭据，否则同链路中间人可直接劫持账号。
class EhCookieStoreIdentityTest {

    private val plainHttpE = URLBuilder(URLProtocol.HTTP, "e-hentai.org").build()
    private val plainHttpEx = URLBuilder(URLProtocol.HTTP, "exhentai.org").build()

    @Test
    fun identityCookiesAreNotSentOverPlainHttp() {
        newIsolatedDataDir()
        try {
            EhCookieStore.setIdentityCookies("1234567", "passhash0123456789", "igneous0123456789")

            val eCookies = getCookieManager().getCookies(plainHttpE) ?: emptyMap()
            assertNull(eCookies[EhCookieStore.KEY_IPB_MEMBER_ID], "member id leaked over http")
            assertNull(eCookies[EhCookieStore.KEY_IPB_PASS_HASH], "pass hash leaked over http")

            val exCookies = getCookieManager().getCookies(plainHttpEx) ?: emptyMap()
            assertNull(exCookies[EhCookieStore.KEY_IGNEOUS], "igneous leaked over http")

            // HTTPS 链路仍可见，签名状态不受影响
            assertTrue(EhCookieStore.hasSignedIn())
            assertEquals("1234567", EhCookieStore.getUserId())
        } finally {
            EhCookieStore.removeAllCookies()
            clearIsolatedDataDir()
        }
    }
}
