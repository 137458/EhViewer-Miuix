package com.ehviewer.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

// 图片保存：阅读器右键「保存」→ 下载目录 ~/Downloads/EhViewer/（不可用时回退 ~/.ehviewer/EhViewer/files/saves/）
object DesktopImageSaver {
    private val IMAGE_EXTENSION = Regex("\\.(jpe?g|png|gif|webp|bmp|avif)$", RegexOption.IGNORE_CASE)

    fun fileNameForUrl(url: String, gid: Long, page: Int): String {
        val segment = url.substringBefore('?').trimEnd('/').substringAfterLast('/')
        val name = if (IMAGE_EXTENSION.containsMatchIn(segment)) {
            segment
        } else {
            "eh_${gid}_p$page.jpg"
        }
        // Windows 文件名非法字符（: \ / * ? " < > |）统一替换为下划线
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }

    fun uniqueTarget(dir: Path, name: String, fs: FileSystem = FileSystem.SYSTEM): Path {
        if (!fs.exists(dir)) throw java.io.IOException("save dir missing: $dir")
        var target = dir / name
        var counter = 1
        val base = name.substringBeforeLast('.', missingDelimiterValue = name)
        val ext = name.substringAfterLast('.', missingDelimiterValue = "")
        while (fs.exists(target)) {
            val candidate = if (ext.isEmpty()) "$base($counter)" else "$base($counter).$ext"
            target = dir / candidate
            counter += 1
        }
        return target
    }

    fun saveDir(): Path {
        val downloads = (System.getProperty("user.home") + "/Downloads/EhViewer").toPath()
        if (runCatching { FileSystem.SYSTEM.createDirectories(downloads) }.isSuccess) return downloads
        val fallback = (System.getProperty("user.home") + "/.ehviewer/EhViewer/files/saves").toPath()
        FileSystem.SYSTEM.createDirectories(fallback)
        return fallback
    }

    // 抓取图片并落盘；网络/IO 异常原样上抛由调用方提示
    suspend fun saveImage(url: String, gid: Long, page: Int, fs: FileSystem = FileSystem.SYSTEM): Path = withContext(Dispatchers.IO) {
        val response = desktopGetBytes(url)
        if (response.status !in 200..299 || response.bytes.isEmpty()) {
            throw java.io.IOException("HTTP ${response.status}")
        }
        val target = uniqueTarget(saveDir(), fileNameForUrl(url, gid, page), fs)
        fs.write(target) { write(response.bytes) }
        target
    }
}
