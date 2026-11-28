package com.hippo.ehviewer.spider

import androidx.collection.MutableObjectIntMap
import androidx.collection.mutableObjectIntMapOf
import arrow.fx.coroutines.fixedRate
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.executeSafely
import com.hippo.ehviewer.ktor.reset
import com.hippo.ehviewer.util.LowSpeedException
import com.hippo.ehviewer.util.ensureSuccess
import io.ktor.client.plugins.ConnectTimeoutException
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import kotlin.concurrent.atomics.AtomicReference
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException

class SpeedTracker(val window: Duration = 1.seconds) {
    private val mutex = Mutex()

    // 采样时间点用单调时钟，墙钟被修改/回拨不会把窗口算成负数或把旧样本当成新样本
    private val origin = TimeSource.Monotonic.markNow()
    private val received = mutableMapOf<Long, Long>()
    private var startNanos: Long? = null

    private fun elapsedNanos() = origin.elapsedNow().inWholeNanoseconds

    suspend fun reset() = mutex.withLock {
        received.clear()
        startNanos = null
    }

    suspend fun track(bytes: Int) = mutex.withLock {
        if (bytes > 0) {
            val now = elapsedNanos()
            startNanos = startNanos ?: now
            // 同一采样时刻可能先后到达多个 chunk，必须累加而不是覆盖
            received[now] = (received[now] ?: 0L) + bytes
        }
    }

    fun speedFlow(sample: Duration = 1.seconds) = fixedRate(sample).mapNotNull {
        mutex.withLock {
            startNanos?.let { start ->
                val now = elapsedNanos()
                val passed = (now - start).nanoseconds
                val window = passed.coerceIn(10.milliseconds, window)
                val cutoff = now - window.inWholeNanoseconds
                val iterator = received.keys.iterator()
                while (iterator.hasNext()) {
                    if (iterator.next() < cutoff) iterator.remove()
                }
                // 用 Long 求和，避免累计字节超过 Int 上限后溢出成负数
                val total = received.values.fold(0L) { acc, v -> acc + v }
                (total / (window / 1.seconds)).toLong()
            }
        }
    }
}

private typealias OnTimeout = (IOException) -> Unit

suspend inline fun <R> timeoutBySpeed(
    url: String,
    crossinline request: suspend (HttpRequestBuilder.() -> Unit) -> HttpStatement,
    crossinline l: suspend (Long, Long, Int) -> Unit,
    crossinline f: suspend (HttpResponse) -> R,
    noinline t: OnTimeout = { throw it },
) = coroutineScope {
    val onTimeout = AtomicReference<OnTimeout?>(t).let { { e: IOException -> it.exchange(null)?.invoke(e) } }
    val watchdog = launch {
        val timeout = 10.seconds
        delay(timeout)
        onTimeout(ConnectTimeoutException(url, timeout.inWholeMilliseconds))
    }
    val tracker = SpeedTracker(2.seconds)
    request {
        var prev = 0L
        onDownload { done, total ->
            // 进度回调可能乱序/重放，增量必须非负；转 Int 前先夹到安全范围
            val delta = (done - prev).coerceAtLeast(0L)
            prev = done
            val bytesRead = delta.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            if (total != null && done == total) {
                tracker.reset()
            } else {
                tracker.track(bytesRead)
            }
            l(total ?: -1L, done, bytesRead)
        }
        timeout { reset() }
    }.executeSafely { resp ->
        watchdog.cancel()
        resp.status.ensureSuccess()
        val speedWatchdog = launch {
            val timeoutSpeed = speedLevelToSpeed(Settings.timeoutSpeed.value) * 1024L
            if (timeoutSpeed > 0) {
                // 3 seconds initial grace period to prevent false-positives during slow-start
                delay(3.seconds)
                var consecutiveLowSpeedCount = 0
                tracker.speedFlow().collect { speed ->
                    if (speed < timeoutSpeed) {
                        consecutiveLowSpeedCount++
                        if (consecutiveLowSpeedCount >= 3) {
                            onTimeout(LowSpeedException(url, speed))
                        }
                    } else {
                        consecutiveLowSpeedCount = 0
                    }
                }
            }
        }
        try {
            f(resp)
        } finally {
            speedWatchdog.cancel()
        }
    }
}

const val MIN_SPEED_LEVEL = 3

fun speedLevelToSpeed(level: Int) = if (level == MIN_SPEED_LEVEL) 0 else 2f.pow(level).roundToInt()

inline fun <T, R> MutableObjectIntMap<T>.fold(initial: R, operation: (acc: R, T, Int) -> R): R {
    var accumulator = initial
    forEach { key, value -> accumulator = operation(accumulator, key, value) }
    return accumulator
}
