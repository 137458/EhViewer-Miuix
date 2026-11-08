package com.hippo.ehviewer.updater

import android.content.Context
import com.ehviewer.core.i18n.R
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

internal enum class UpdateFailureKind { Network, Http, Storage, Unknown }

internal object UpdateFailureClassifier {
    private val HTTP_PATTERN = Regex("""HTTP\s+(\d{3})""", RegexOption.IGNORE_CASE)
    private val STORAGE_PATTERN = Regex(
        """ENOSPC|No space left|EACCES|Permission denied|Read-only file system|open failed|无法创建|无法清理""",
        RegexOption.IGNORE_CASE,
    )
    private val NETWORK_PATTERN = Regex(
        """timeout|timed out|connect|unable to resolve host|network|unreachable|connection reset|broken pipe|\bEOF\b|closed""",
        RegexOption.IGNORE_CASE,
    )

    fun httpStatus(error: Throwable): Int? =
        messages(error).firstNotNullOfOrNull { message ->
            HTTP_PATTERN.find(message)?.groupValues?.get(1)?.toIntOrNull()
        }

    fun kind(error: Throwable): UpdateFailureKind {
        val messages = messages(error)
        if (messages.any { HTTP_PATTERN.containsMatchIn(it) }) return UpdateFailureKind.Http

        var current: Throwable? = error
        while (current != null) {
            when (current) {
                is UnknownHostException, is ConnectException, is SocketTimeoutException, is SSLException ->
                    return UpdateFailureKind.Network
            }
            val cause = current.cause
            current = if (cause == current) null else cause
        }

        if (messages.any { STORAGE_PATTERN.containsMatchIn(it) }) return UpdateFailureKind.Storage
        if (messages.any { NETWORK_PATTERN.containsMatchIn(it) }) return UpdateFailureKind.Network
        return UpdateFailureKind.Unknown
    }

    private fun messages(error: Throwable): List<String> {
        val result = mutableListOf<String>()
        var current: Throwable? = error
        while (current != null) {
            current.message?.let(result::add)
            val cause = current.cause
            current = if (cause == current) null else cause
        }
        return result
    }
}

internal data class UpdateFailureText(val resId: Int, val args: List<String>)

/** 文案与 Context 解耦，纯逻辑可单测；格式化交给调用方。 */
internal fun updateFailureText(error: Throwable): UpdateFailureText =
    when (UpdateFailureClassifier.kind(error)) {
        UpdateFailureKind.Network -> UpdateFailureText(R.string.update_error_network, emptyList())
        UpdateFailureKind.Http -> UpdateFailureText(
            R.string.update_error_http,
            listOf(UpdateFailureClassifier.httpStatus(error)?.toString().orEmpty()),
        )
        UpdateFailureKind.Storage -> UpdateFailureText(R.string.update_error_storage, emptyList())
        UpdateFailureKind.Unknown -> UpdateFailureText(
            R.string.update_error_unknown,
            listOf(error.localizedMessage ?: error.javaClass.simpleName),
        )
    }

internal fun updateDigestFailureText(expected: String, actual: String): UpdateFailureText =
    UpdateFailureText(R.string.update_error_digest, listOf(expected.take(12), actual.take(12)))

internal fun updateFailureMessage(context: Context, error: Throwable): String =
    with(updateFailureText(error)) { context.getString(resId, *args.toTypedArray()) }
