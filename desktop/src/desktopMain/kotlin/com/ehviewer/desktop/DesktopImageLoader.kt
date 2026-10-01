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
        return ImageLoader.Builder(context)
            .diskCache(buildDiskCache(targetCacheDir))
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
