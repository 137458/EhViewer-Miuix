package com.ehviewer.core.database.client

// 登录页解析（下沉自 app 模块 SignInParser）。
// 异常语义建模为三分支 outcome，由调用方（平台侧）转换为各自异常类型。
object SignInParser {
    private val NAME_PATTERN = Regex("<p>You are now logged in as: (.+?)<")
    private val ERROR_PATTERN = Regex(
        "<h4>The error returned was:</h4>\\s*<p>(.+?)</p>" +
            "|<span class=\"postcolor\">(.+?)</span>",
    )

    sealed interface SignInParseOutcome {
        data class Success(val name: String) : SignInParseOutcome
        data class SiteError(val message: String) : SignInParseOutcome
        data object NotFound : SignInParseOutcome
    }

    fun parse(body: String): SignInParseOutcome {
        NAME_PATTERN.find(body)?.let {
            return SignInParseOutcome.Success(it.groupValues[1])
        }
        ERROR_PATTERN.find(body)?.let {
            val message = it.groupValues[1].ifEmpty { it.groupValues[2] }
            return SignInParseOutcome.SiteError(message)
        }
        return SignInParseOutcome.NotFound
    }
}
