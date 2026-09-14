package com.hippo.ehviewer.util

import com.hippo.ehviewer.client.exception.QuotaExceededException
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.utils.io.charsets.decode
import java.nio.charset.CodingErrorAction
import kotlinx.io.Source

suspend fun HttpResponse.bodyAsUtf8Text(): String {
    val decoder = Charsets.UTF_8.newDecoder().apply {
        onMalformedInput(CodingErrorAction.REPLACE)
        onUnmappableCharacter(CodingErrorAction.REPLACE)
    }
    val input = body<Source>()

    return decoder.decode(input)
}

fun HttpStatusCode.ensureSuccess() {
    if (!isSuccess()) {
        if (value == 509) {
            throw QuotaExceededException()
        }
        throw Exception(toString())
    }
}
