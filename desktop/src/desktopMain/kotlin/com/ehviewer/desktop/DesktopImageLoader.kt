package com.ehviewer.desktop

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy
import okio.Path.Companion.toPath

object DesktopImageLoader {
    private const val DEFAULT_DISK_CACHE_SIZE = 250L * 1024 * 1024 // 250 MB

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

    fun defaultCacheDirectory(): File {
        val base = System.getProperty("ehviewer.data.dir")?.takeIf { it.isNotBlank() }
            ?: System.getenv("APPDATA")
            ?: (System.getProperty("user.home") + File.separator + ".ehviewer")
        return File(base, "EhViewer" + File.separator + "cache" + File.separator + "image_cache")
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
        val targetCacheDir = cacheDir ?: defaultCacheDirectory()
        val cacheSize = runCatching { adaptiveCacheSizeBytes(targetCacheDir.usableSpace) }
            .getOrDefault(DEFAULT_DISK_CACHE_SIZE)
        return ImageLoader.Builder(context)
            .diskCache(buildDiskCache(targetCacheDir, maxSizeBytes = cacheSize))
            .components {
                add(
                    KtorNetworkFetcherFactory(
                        httpClient = {
                            HttpClient(OkHttp) {
                                engine {
                                    val proxyConfig = DesktopSettings.proxy.value
                                    if (!proxyConfig.isNullOrBlank()) {
                                        val parts = proxyConfig.split(":")
                                        if (parts.size == 2) {
                                            val host = parts[0]
                                            val port = parts[1].toIntOrNull()
                                            if (port != null) {
                                                proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port))
                                            }
                                        }
                                    }
                                }
                            }
                        },
                    ),
                )
            }
            .build()
    }

    fun init() {
        SingletonImageLoader.setSafe {
            newImageLoader()
        }
    }
}
