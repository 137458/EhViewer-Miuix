package com.ehviewer.desktop

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import java.io.File
import okio.Path.Companion.toPath

object DesktopImageLoader {
    private const val DEFAULT_DISK_CACHE_SIZE = 250L * 1024 * 1024 // 250 MB

    // -Xmx512m 下内存缓存按堆 25% 封顶：大图逐页翻阅时不会把整个堆吃掉
    private const val MEMORY_CACHE_SIZE_PERCENT = 0.25

    private const val CACHE_DIR_NAME = "image_cache"

    private val loaderLock = Any()
    private var registeredLoader: ImageLoader? = null
    private var registeredProxyKey: String? = null

    // 重建 loader 时复用同一份磁盘/内存缓存实例：不能 shutdown 旧 loader（会关掉共享缓存句柄），
    // 丢引用后其请求上下文由 GC 回收
    private val sharedCaches: SharedCaches by lazy { SharedCaches() }

    private class SharedCaches {
        val directory: File = defaultCacheDirectory()
        val diskCache: DiskCache = buildDiskCache(directory, maxSizeBytes = cacheSizeFor(directory))
        val memoryCache: MemoryCache = MemoryCache.Builder()
            .maxSizePercent(PlatformContext.INSTANCE, MEMORY_CACHE_SIZE_PERCENT)
            .build()
    }

    // 与 Android 侧 EhApplication.thumbCache 三档公式一致：>10G→512M，>2G→256M，否则 128M
    fun adaptiveCacheSizeBytes(freeSpaceBytes: Long): Long {
        val mb = 1024L * 1024
        val gb = 1024L * mb
        return when {
            freeSpaceBytes > 10 * gb -> 512 * mb
            freeSpaceBytes > 2 * gb -> 256 * mb
            freeSpaceBytes >= 0 -> 128 * mb
            else -> 256 * mb
        }
    }

    fun defaultCacheDirectory(): File = File(DesktopDirs.cacheRoot, CACHE_DIR_NAME)

    // 磁盘配额按可用空间分档。注意顺序：目录不存在时 usableSpace 返回 0（会被压到 128M 档），
    // 故先 mkdirs；仍建不出来（只读介质/权限）就退到最近的已存在祖先探测
    internal fun cacheSizeFor(cacheDir: File): Long {
        if (!cacheDir.isDirectory) cacheDir.mkdirs()
        val probe = nearestExisting(cacheDir)
        return adaptiveCacheSizeBytes(probe?.usableSpace ?: -1L)
    }

    internal fun nearestExisting(dir: File): File? {
        var current: File? = dir
        while (current != null) {
            if (current.exists()) return current
            current = current.parentFile
        }
        return null
    }

    fun buildDiskCache(
        cacheDir: File,
        maxSizeBytes: Long = DEFAULT_DISK_CACHE_SIZE,
    ): DiskCache = DiskCache.Builder()
        .directory(cacheDir.absolutePath.toPath())
        .maxSizeBytes(maxSizeBytes)
        .build()

    fun newImageLoader(
        context: PlatformContext = PlatformContext.INSTANCE,
        cacheDir: File? = null,
    ): ImageLoader {
        if (cacheDir == null) {
            return ImageLoader.Builder(context)
                .diskCache(sharedCaches.diskCache)
                .memoryCache(sharedCaches.memoryCache)
                .components { add(networkFetcherFactory()) }
                .build()
        }
        // 注入目录（测试/便携模式）：独立缓存实例，大小同样按该目录所在卷的可用空间分档
        return ImageLoader.Builder(context)
            .diskCache(buildDiskCache(cacheDir, maxSizeBytes = cacheSizeFor(cacheDir)))
            .memoryCache(
                MemoryCache.Builder()
                    .maxSizePercent(context, MEMORY_CACHE_SIZE_PERCENT)
                    .build(),
            )
            .components { add(networkFetcherFactory()) }
            .build()
    }

    // 图片请求走 NetworkModule 的共享客户端：EhCookieStore（ex/ex 图片需登录态）、UA、
    // 超时、代理与重定向规则全部与页面/API 请求一致，不另建一套网络栈
    private fun networkFetcherFactory(): coil3.network.NetworkFetcher.Factory = KtorNetworkFetcherFactory(httpClient = { acquireClient() })

    fun init() {
        SingletonImageLoader.setSafe {
            val loader = newImageLoader()
            synchronized(loaderLock) {
                registeredLoader = loader
                registeredProxyKey = DesktopSettings.proxy.value
            }
            loader
        }
    }

    // 代理变化时重建 loader：Coil 的 NetworkClient 在 factory 内惰性只建一次，
    // 无法就地换引擎代理，只能换 loader 实例（缓存实例复用，切换瞬间在飞请求不受影响）
    fun refreshNetworkIfProxyChanged(): Boolean = synchronized(loaderLock) {
        val proxyKey = DesktopSettings.proxy.value
        if (registeredLoader == null) return false
        if (proxyKey == registeredProxyKey) return false
        val loader = newImageLoader()
        // setSafe 只在未注册时生效，替换必须用 setUnsafe
        SingletonImageLoader.setUnsafe(loader)
        registeredLoader = loader
        registeredProxyKey = proxyKey
        true
    }
}
