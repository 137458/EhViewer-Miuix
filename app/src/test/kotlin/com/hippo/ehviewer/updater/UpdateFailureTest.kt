package com.hippo.ehviewer.updater

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpdateFailureTest {
    @Test
    fun `classifies host and connection failures as network`() {
        assertEquals(UpdateFailureKind.Network, UpdateFailureClassifier.kind(UnknownHostException("ehviewer.invalid")))
        assertEquals(UpdateFailureKind.Network, UpdateFailureClassifier.kind(ConnectException("refused")))
        assertEquals(UpdateFailureKind.Network, UpdateFailureClassifier.kind(SocketTimeoutException("read timed out")))
    }

    @Test
    fun `classifies io message variants as network`() {
        assertEquals(UpdateFailureKind.Network, UpdateFailureClassifier.kind(IOException("Unable to resolve host")))
        assertEquals(UpdateFailureKind.Network, UpdateFailureClassifier.kind(IOException("Connection reset")))
    }

    @Test
    fun `extracts http status and classifies it as http`() {
        val error = IOException("Server returned HTTP 403")

        assertEquals(403, UpdateFailureClassifier.httpStatus(error))
        assertEquals(UpdateFailureKind.Http, UpdateFailureClassifier.kind(error))
        assertNull(UpdateFailureClassifier.httpStatus(IOException("plain failure")))
    }

    @Test
    fun `classifies storage failures`() {
        assertEquals(UpdateFailureKind.Storage, UpdateFailureClassifier.kind(IOException("No space left on device")))
        assertEquals(UpdateFailureKind.Storage, UpdateFailureClassifier.kind(IOException("EACCES: permission denied")))
    }

    @Test
    fun `walks the cause chain`() {
        val nested = RuntimeException("wrap", IOException("ENOSPC"))

        assertEquals(UpdateFailureKind.Storage, UpdateFailureClassifier.kind(nested))
    }

    @Test
    fun `falls back to unknown`() {
        assertEquals(UpdateFailureKind.Unknown, UpdateFailureClassifier.kind(IllegalStateException("boom")))
    }
}
