package com.ehviewer.core.network

import io.ktor.http.Cookie
import io.ktor.http.Url
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// 桌面侧 CookieManager actual 的运行时验证：写读回环 + 同步落盘
class CookieManagerDesktopTest {
    @Test
    fun setCookieRoundtripAndPersistToDisk() {
        val manager = getCookieManager()
        val url = Url("https://exhentai.org")
        val name = "smoke_cookie_${System.currentTimeMillis()}"
        manager.setCookie(url, Cookie(name = name, value = "42", domain = ".exhentai.org"))

        assertEquals("42", manager.getCookies(url)?.get(name))
        manager.flush()

        // PersistentCookieStore 在变更时同步写盘；路径与实现内 cookieStorePath 约定一致，
        // 落盘内容为序列化 CookieRecord，ASCII 字段名可直接在字节流中检索
        val base = System.getenv("APPDATA")?.replace('\\', '/')
            ?: (System.getProperty("user.home") + "/.ehviewer")
        val bytes = File("$base/EhViewer/files/cookies.dat").readBytes()
        assertTrue(name in String(bytes, Charsets.ISO_8859_1), "persisted store should contain $name")
    }
}
