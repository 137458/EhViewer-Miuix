package com.hippo.ehviewer.coil

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import androidx.core.graphics.createBitmap
import com.ehviewer.core.util.logcat
import java.io.Closeable
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

// Hold a reference to the buffer as it's used by the decoder
@Suppress("CanBeParameter")
class AnimatedWebPDrawable(private val source: ByteBuffer) : Drawable(), Animatable, Closeable {
    private val decodeScope = CoroutineScope(Dispatchers.IO.limitedParallelism(1))
    private val mainScope = CoroutineScope(Dispatchers.Main.immediate)
    private val disposeScope = CoroutineScope(Dispatchers.IO)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG)

    // 解码器句柄放进 AtomicLong：dispose() 与解码协程可能跑在不同线程上，
    // getAndSet(0L) 保证 native 侧只被销毁一次，也保证销毁后解码协程不会再用野指针。
    private val decoder = AtomicLong(nativeCreateDecoder(source))
    private val width: Int
    private val height: Int
    private val loopCount: Int

    private var decodeJob: Job? = null
    private var loopsCompleted = 0
    private var timeToShowNextFrame = 0L
    private var currentFrame: Frame
    private var nextFrame: Frame

    init {
        try {
            val handle = decoder.get()
            check(handle != 0L) { "Failed to create decoder" }
            val packed = nativeGetImageInfo(handle)
            width = (packed shr 40).toInt()
            height = (packed shr 16 and 0xFFFFFF).toInt()
            loopCount = (packed and 0xFFFF).toInt()
            val bitmap = createBitmap(width, height)
            val timestamp = nativeDecodeNextFrame(handle, false, bitmap)
            check(timestamp != 0) { "Failed to decode first frame" }
            currentFrame = Frame(bitmap, timestamp)
            nextFrame = Frame(createBitmap(width, height), 0)
        } catch (e: Throwable) {
            // createBitmap 可能抛 OOM：构造失败也必须把 native 解码器还回去，否则每失败一次泄漏一块 Rust 缓冲
            destroyDecoder()
            throw e
        }
        // Coil 内存缓存驱逐不会调用 dispose()，靠引用队列兜底回收 native 解码器
        DecoderCleaner.register(this, decoder)
    }

    override fun getIntrinsicWidth() = width

    override fun getIntrinsicHeight() = height

    private val runnable = Runnable {
        timeToShowNextFrame = SystemClock.uptimeMillis() + nextFrame.timestamp - currentFrame.timestamp
        invalidateSelf()
    }

    private fun decodeNextFrame(reset: Boolean) = decodeScope.launch {
        val handle = decoder.get()
        if (handle == 0L) return@launch // 已 dispose，别碰已释放的 native 解码器
        val timestamp = nativeDecodeNextFrame(handle, reset, nextFrame.bitmap)
        ensureActive()
        check(timestamp != 0) {
            decodeJob = null
            "Failed to decode next frame"
        }
        if (timestamp <= currentFrame.timestamp) {
            if (reset) loopsCompleted = 0 else loopsCompleted++
            currentFrame.timestamp = 0
        }
        nextFrame.timestamp = timestamp
        nextFrame.bitmap.prepareToDraw()
    }.apply {
        invokeOnCompletion { cause ->
            when (cause) {
                // 完成回调跑在 IO 线程上，invalidateSelf/scheduleSelf 只能在主线程调用
                null -> mainScope.launch {
                    if (reset) {
                        runnable.run()
                    } else {
                        scheduleSelf(runnable, timeToShowNextFrame)
                    }
                }
                !is CancellationException -> logcat(cause)
            }
        }
    }

    override fun draw(canvas: Canvas) {
        if (decodeJob?.isCompleted == true && isVisible) {
            decodeJob = if (loopCount == 0 || loopsCompleted < loopCount) {
                currentFrame = nextFrame.also { nextFrame = currentFrame }
                decodeNextFrame(false)
            } else {
                null
            }
        }
        canvas.drawBitmap(currentFrame.bitmap, null, bounds, paint)
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun setAlpha(alpha: Int) {
        if (alpha != paint.alpha) {
            paint.alpha = alpha
            invalidateSelf()
        }
    }

    override fun getAlpha(): Int = paint.alpha

    override fun setColorFilter(colorFilter: ColorFilter?) {
        if (colorFilter != paint.colorFilter) {
            paint.colorFilter = colorFilter
            invalidateSelf()
        }
    }

    override fun getColorFilter(): ColorFilter? = paint.colorFilter

    override fun isRunning() = decodeJob != null

    override fun start() {
        if (decodeJob == null) {
            decodeJob = decodeNextFrame(true)
        }
    }

    override fun stop() {
        decodeJob?.cancel()
        decodeJob = null
        unscheduleSelf(runnable)
    }

    private fun destroyDecoder() {
        val handle = decoder.getAndSet(0L)
        if (handle != 0L) {
            nativeDestroyDecoder(handle)
        }
    }

    fun dispose() {
        unscheduleSelf(runnable)
        stop()
        // 销毁 native 解码器等解码协程收尾需要阻塞；放到 IO 上做，避免主线程 runBlocking 卡 UI
        disposeScope.launch {
            decodeScope.coroutineContext.job.cancelAndJoin()
            destroyDecoder()
            mainScope.coroutineContext.job.cancel()
        }
    }

    override fun close() = dispose()
}

/**
 * 弱引用兜底清理：Coil 把解码结果放进内存缓存后，驱逐时只丢引用、不会调用 [AnimatedWebPDrawable.dispose]，
 * 光靠显式释放会让 native 解码器（Rust 侧缓冲）随每次重新解码泄漏。这里用引用队列守护，
 * 对象被 GC 时销毁解码器；显式 dispose 过的句柄已被置 0，重复销毁是空操作。
 */
private class PendingDecoderCleanup(
    drawable: AnimatedWebPDrawable,
    private val decoder: AtomicLong,
    queue: ReferenceQueue<AnimatedWebPDrawable>,
) : WeakReference<AnimatedWebPDrawable>(drawable, queue) {
    fun clean() {
        val handle = decoder.getAndSet(0L)
        if (handle != 0L) {
            nativeDestroyDecoder(handle)
        }
    }
}

private object DecoderCleaner {
    private val queue = ReferenceQueue<AnimatedWebPDrawable>()
    private val pending = ConcurrentHashMap.newKeySet<PendingDecoderCleanup>()

    @Volatile
    private var thread: Thread? = null

    fun register(drawable: AnimatedWebPDrawable, decoder: AtomicLong) {
        if (thread == null) {
            synchronized(this) {
                if (thread == null) {
                    thread = Thread(::loop, "AnimatedWebP-cleaner").apply {
                        isDaemon = true
                        start()
                    }
                }
            }
        }
        pending += PendingDecoderCleanup(drawable, decoder, queue)
    }

    private fun loop() {
        while (true) {
            val cleanup = queue.remove() as? PendingDecoderCleanup ?: continue
            pending -= cleanup
            cleanup.clean()
        }
    }
}

private class Frame(val bitmap: Bitmap, var timestamp: Int)

private external fun nativeCreateDecoder(source: ByteBuffer): Long
private external fun nativeGetImageInfo(decoder: Long): Long
private external fun nativeDecodeNextFrame(decoder: Long, reset: Boolean, bitmap: Bitmap): Int
private external fun nativeDestroyDecoder(decoder: Long)
