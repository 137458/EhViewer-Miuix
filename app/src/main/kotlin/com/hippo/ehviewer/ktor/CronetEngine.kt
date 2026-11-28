@file:RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)

package com.hippo.ehviewer.ktor

import android.net.http.HttpEngine
import android.net.http.HttpException
import android.net.http.UploadDataProvider
import android.net.http.UploadDataSink
import android.net.http.UrlRequest
import android.net.http.UrlResponseInfo
import android.os.Build
import androidx.annotation.RequiresExtension
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.callContext
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headers
import io.ktor.util.date.GMTDate
import io.ktor.util.flattenForEach
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.pool.DirectByteBufferPool
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writer
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

class CronetEngine(override val config: CronetConfig) : HttpClientEngineBase("Cronet") {
    // Limit thread to 1 since we are async & non-blocking
    // TODO: Investigate https://github.com/FooIbar/EhViewer/issues/2854
    // override val dispatcher = Dispatchers.Default.limitedParallelism(1)

    override val supportedCapabilities = setOf(HttpTimeoutCapability)

    private val executor = dispatcher.limitedParallelism(1).asExecutor()
    private val pool = DirectByteBufferPool(32)
    private val client by lazy {
        with(config) { HttpEngine.Builder(context).apply(config).build() }
    }

    @InternalAPI
    override suspend fun execute(data: HttpRequestData) = executeHttpRequest(callContext(), data)

    private suspend fun executeHttpRequest(
        callContext: CoroutineContext,
        data: HttpRequestData,
    ) = suspendCancellableCoroutine { continuation ->
        val requestTime = GMTDate()
        // Single slot: Cronet has at most one outstanding read and the reader copies a chunk out
        // before scheduling the next one, so a tiny bounded queue keeps callbacks non-blocking.
        val chunkChan = Channel<ByteBuffer>(1)
        // Signalled from every Cronet callback so the timeout watchdogs can tell "nothing received
        // yet" from "the connection went silent mid transfer" without polling.
        val activity = Channel<Unit>(Channel.CONFLATED)
        val bufferRef = AtomicReference<ByteBuffer?>()
        // The pooled buffer has two owners: the body writer and the terminal Cronet callback. It may
        // only go back to the pool when both are done, otherwise Cronet can still write into it.
        val bufferOwners = AtomicInteger(2)
        val terminalReached = AtomicBoolean(false)
        val responseStarted = AtomicBoolean(false)
        // Detached from callContext.job: a structured child would keep the request job alive while
        // the watchdog waits for its next activity signal.
        val watchdogScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        fun releaseBufferOwner() {
            if (bufferOwners.decrementAndGet() == 0) {
                bufferRef.getAndSet(null)?.let(pool::recycle)
            }
        }

        fun onTerminal() {
            if (terminalReached.compareAndSet(false, true)) releaseBufferOwner()
        }

        val callback = object : UrlRequest.Callback {
            override fun onRedirectReceived(request: UrlRequest, info: UrlResponseInfo, newLocationUrl: String) {
                // Ktor follows redirects itself, so Cronet must not continue to the new location.
                request.cancel()
                if (continuation.isActive) {
                    continuation.resume(
                        info.toHttpResponseData(
                            requestTime = requestTime,
                            callContext = callContext,
                        ),
                    )
                }
            }

            override fun onResponseStarted(request: UrlRequest, info: UrlResponseInfo) {
                responseStarted.set(true)
                activity.trySend(Unit)
                if (!continuation.isActive) {
                    request.cancel()
                    return
                }
                continuation.resume(
                    info.toHttpResponseData(
                        requestTime = requestTime,
                        callContext = callContext,
                        responseBody = writer(callContext.job) {
                            val buffer = pool.borrow()
                            bufferRef.set(buffer)
                            try {
                                request.read(buffer)
                                chunkChan.consumeEach { filled ->
                                    filled.flip()
                                    channel.writeFully(filled)
                                    filled.clear()
                                    request.read(filled)
                                }
                            } finally {
                                releaseBufferOwner()
                            }
                        }.channel,
                    ),
                )
            }

            override fun onReadCompleted(request: UrlRequest, info: UrlResponseInfo, byteBuffer: ByteBuffer) {
                activity.trySend(Unit)
                // Never block a Cronet network thread: if the consumer cannot take the chunk the
                // request is either cancelled or gone, so abort instead of stalling here.
                if (chunkChan.trySend(byteBuffer).isFailure) {
                    request.cancel()
                }
            }

            override fun onSucceeded(request: UrlRequest, info: UrlResponseInfo) {
                chunkChan.close()
                onTerminal()
            }

            override fun onFailed(request: UrlRequest, info: UrlResponseInfo?, error: HttpException) {
                chunkChan.close(error)
                if (continuation.isActive) {
                    continuation.resumeWithException(error)
                }
                onTerminal()
            }

            override fun onCanceled(request: UrlRequest, info: UrlResponseInfo?) {
                chunkChan.close(CancellationException("Cronet request was cancelled"))
                onTerminal()
            }
        }

        val request = client.newUrlRequestBuilder(data.url.toString(), executor, callback).apply {
            setHttpMethod(data.method.value)
            data.headers.flattenForEach { key, value -> addHeader(key, value) }
            data.body.contentType?.let { addHeader(HttpHeaders.ContentType, it.toString()) }
            data.body.contentLength?.let { addHeader(HttpHeaders.ContentLength, it.toString()) }
            data.body.toUploadDataProvider()?.let { setUploadDataProvider(it, executor) }
        }.build()

        request.start()
        callContext.job.invokeOnCompletion {
            watchdogScope.cancel()
            request.cancel()
        }

        // UrlRequest.Builder has no timeout setters, so connect / socket timeouts are enforced here
        // by cancelling the request once the expected activity signal stops arriving.
        val timeoutConfig = data.getCapabilityOrNull(HttpTimeoutCapability)
        val connectTimeout = (timeoutConfig?.connectTimeoutMillis ?: timeoutConfig?.requestTimeoutMillis)
            ?.takeIf { it > 0 }
        val socketTimeout = (timeoutConfig?.socketTimeoutMillis ?: timeoutConfig?.requestTimeoutMillis)
            ?.takeIf { it > 0 }
        if (connectTimeout != null || socketTimeout != null) {
            watchdogScope.launch {
                var waitingForResponse = true
                while (true) {
                    val timeout = if (waitingForResponse) connectTimeout else socketTimeout
                    val signalled = if (timeout == null) {
                        activity.receive()
                        true
                    } else {
                        withTimeoutOrNull(timeout) { activity.receive() } != null
                    }
                    if (!signalled) {
                        request.cancel()
                        return@launch
                    }
                    if (waitingForResponse && responseStarted.get()) waitingForResponse = false
                }
            }
        }
    }
}

private fun UrlResponseInfo.toHttpResponseData(
    requestTime: GMTDate,
    callContext: CoroutineContext,
    responseBody: ByteReadChannel = ByteReadChannel.Empty,
) = HttpResponseData(
    statusCode = HttpStatusCode.fromValue(httpStatusCode),
    requestTime = requestTime,
    headers = headers {
        headers.asMap.forEach { (key, value) ->
            appendAll(key, value)
        }
    },
    version = when (negotiatedProtocol) {
        "h2" -> HttpProtocolVersion.HTTP_2_0
        "h3" -> HttpProtocolVersion.QUIC
        "quic/1+spdy/3" -> HttpProtocolVersion.SPDY_3
        else -> HttpProtocolVersion.HTTP_1_1
    },
    body = responseBody,
    callContext = callContext,
)

private fun OutgoingContent.toUploadDataProvider() = when (this) {
    is OutgoingContent.NoContent -> null
    is OutgoingContent.ByteArrayContent -> object : UploadDataProvider() {
        val buffer = ByteBuffer.wrap(bytes()).slice()
        private val length = buffer.limit()
        override fun getLength() = length.toLong()
        override fun read(uploadDataSink: UploadDataSink, byteBuffer: ByteBuffer) {
            // The sink buffer owned by Cronet is small (16 KiB), so a single put would overflow
            // for larger bodies. Copy at most what both sides can still take.
            val size = minOf(byteBuffer.remaining(), buffer.remaining())
            byteBuffer.put(buffer.duplicate().apply { limit(position() + size) })
            buffer.position(buffer.position() + size)
            uploadDataSink.onReadSucceeded(buffer.remaining() == 0)
        }
        override fun rewind(uploadDataSink: UploadDataSink) {
            buffer.position(0)
            uploadDataSink.onRewindSucceeded()
        }
    }
    else -> error("UnsupportedContentType $this")
}
