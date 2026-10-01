package com.ehviewer.core.database.client

import kotlin.test.Test
import kotlin.test.assertEquals

// 桌面侧登录页解析下沉验证（成功用户名 / 站点错误消息 / 不可解析三分支）
class SignInParserTest {

    @Test
    fun parseSuccessPageExtractsUsername() {
        val body = """
            <p>You are now logged in as: someuser123<</p>
            <p>Always use this exact name to login.</p>
        """.trimIndent()
        val outcome = SignInParser.parse(body)
        assertEquals(SignInParser.SignInParseOutcome.Success("someuser123"), outcome)
    }

    @Test
    fun parseSiteErrorBlockReturnsSiteErrorMessage() {
        val body = """
            <h4>The error returned was:</h4>
            <p>Invalid username or password</p>
        """.trimIndent()
        val outcome = SignInParser.parse(body)
        assertEquals(
            SignInParser.SignInParseOutcome.SiteError("Invalid username or password"),
            outcome,
        )
    }

    @Test
    fun parsePostColorErrorReturnsMessage() {
        val body = """<span class="postcolor">You must enter your password</span>"""
        val outcome = SignInParser.parse(body)
        assertEquals(
            SignInParser.SignInParseOutcome.SiteError("You must enter your password"),
            outcome,
        )
    }

    @Test
    fun parseUnrelatedPageReturnsNotFound() {
        assertEquals(SignInParser.SignInParseOutcome.NotFound, SignInParser.parse("<html></html>"))
        assertEquals(SignInParser.SignInParseOutcome.NotFound, SignInParser.parse(""))
    }
}
