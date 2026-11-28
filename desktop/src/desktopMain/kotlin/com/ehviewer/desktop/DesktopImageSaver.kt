package com.ehviewer.desktop

import com.ehviewer.core.i18n.MR
import dev.icerock.moko.resources.desc.ResourceFormatted
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

// 图片保存：阅读器右键「保存」→ 下载目录 ~/Downloads/EhViewer/（不可用时回退应用数据区 files/saves/）
object DesktopImageSaver {
    private val IMAGE_EXTENSION = Regex("\\.(jpe?g|png|gif|webp|bmp|avif)$", RegexOption.IGNORE_CASE)

    // Windows 非法字符 + 控制字符一并替换：文件名会被直接拼进目录，任何分隔符都可能改变落点
    private val ILLEGAL_NAME_CHARS = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")

    // 文件名单段长度上限：给 "（n）" 去重后缀与扩展名留余量（各文件系统上限多为 255 字节）
    private const val MAX_FILE_NAME_LENGTH = 120

    private const val TEMP_SUFFIX = ".tmp"

    private const val DEFAULT_SAVE_DIR_NAME = "Downloads/EhViewer"

    // Windows 设备名：即便带扩展名（NUL.jpg）也会被系统当作设备打开
    private val WINDOWS_RESERVED_NAMES: Set<String> = buildSet {
        listOf("CON", "PRN", "AUX", "NUL").forEach { add(it) }
        (1..9).forEach {
            add("COM$it")
            add("LPT$it")
        }
    }

    // 默认保存目录（纯字符串计算，无磁盘 IO）：设置页展示与 resolveSaveDir 共用同一规则
    fun defaultSaveDirPath(userHome: String): String = "$userHome/$DEFAULT_SAVE_DIR_NAME"

    // 兜底目录：应用数据区（DesktopDirs 已按平台归一）
    fun fallbackSaveDirPath(): String = DesktopDirs.dataRoot.absolutePath.replace('\\', '/') + "/files/saves"

    fun isWindowsReservedName(name: String): Boolean {
        val base = name.substringBeforeLast('.', missingDelimiterValue = name).trim().uppercase()
        return base in WINDOWS_RESERVED_NAMES
    }

    // 文件名单段化：替换非法字符、去尾随空格/点、保留名加前缀、超长截断
    internal fun sanitizeFileName(raw: String): String {
        val replaced = raw.replace(ILLEGAL_NAME_CHARS, "_").trim().trimEnd('.', ' ')
        val candidate = replaced.ifEmpty { "image" }
        val base = candidate.substringBeforeLast('.', missingDelimiterValue = candidate)
        val ext = candidate.substringAfterLast('.', missingDelimiterValue = "")
        val safeBase = if (isWindowsReservedName(base)) "_$base" else base
        val withExt = if (ext.isEmpty() || base == candidate) safeBase else safeBase + "." + ext
        if (withExt.length <= MAX_FILE_NAME_LENGTH) return withExt
        // 截断保留扩展名：否则超长文件名落盘后没有可识别后缀
        val suffix = if (ext.isEmpty() || base == candidate) "" else ".$ext"
        if (suffix.length >= MAX_FILE_NAME_LENGTH) return withExt.take(MAX_FILE_NAME_LENGTH)
        return safeBase.take(MAX_FILE_NAME_LENGTH - suffix.length) + suffix
    }

    // 目标必须是 dir 下的单段文件名：拒绝分隔符、"." / ".."、非法字符与超长
    internal fun isValidSingleSegmentName(name: String): Boolean {
        if (name.isEmpty() || name == "." || name == "..") return false
        // 首尾空白/纯空白会被 Windows 的 Paths.get 拒绝（InvalidPathException），必须在拼接路径前判非法
        if (name != name.trim()) return false
        if (name.contains('/') || name.contains('\\')) return false
        if (ILLEGAL_NAME_CHARS.containsMatchIn(name)) return false
        return name.length <= 255
    }

    fun fileNameForUrl(url: String, gid: Long, page: Int): String {
        val segment = url.substringBefore('?').trimEnd('/').substringAfterLast('/')
        val name = if (IMAGE_EXTENSION.containsMatchIn(segment)) {
            segment
        } else {
            "eh_${gid}_p$page.jpg"
        }
        return sanitizeFileName(name)
    }

    fun uniqueTarget(dir: Path, name: String, fs: FileSystem = FileSystem.SYSTEM): Path {
        if (!fs.exists(dir)) {
            // 该异常会经阅读器保存失败通知直接展示给用户，文案走本地化资源
            val message = StringDesc.ResourceFormatted(MR.strings.desktop_save_dir_missing, dir.toString()).localized()
            throw java.io.IOException(message)
        }
        if (!isValidSingleSegmentName(name)) {
            throw java.io.IOException("Invalid file name: $name")
        }
        var target = dir / name
        // 规范化后父目录必须仍是 dir：拒绝一切逃出保存目录的落点
        if (target.parent != dir) throw java.io.IOException("Refusing to write outside save dir: $target")
        var counter = 1
        val base = name.substringBeforeLast('.', missingDelimiterValue = name)
        val ext = name.substringAfterLast('.', missingDelimiterValue = "")
        while (fs.exists(target)) {
            val candidate = if (ext.isEmpty() || base == name) "$base($counter)" else "$base($counter).$ext"
            target = dir / candidate
            if (target.parent != dir) throw java.io.IOException("Refusing to write outside save dir: $target")
            counter += 1
        }
        return target
    }

    fun saveDir(): Path = resolveSaveDir(DesktopSettings.imageSaveDir.value, System.getProperty("user.home"))

    // 目录解析：设置项优先 → 默认下载目录 → 应用数据区兜底；上级不可建时逐级回落
    fun resolveSaveDir(configured: String?, userHome: String, fs: FileSystem = FileSystem.SYSTEM): Path {
        if (!configured.isNullOrBlank()) {
            val custom = configured.replace('\\', '/').trimEnd('/').toPath()
            if (runCatching { fs.createDirectories(custom) }.isSuccess) return custom
        }
        val downloads = defaultSaveDirPath(userHome).toPath()
        if (runCatching { fs.createDirectories(downloads) }.isSuccess) return downloads
        val fallback = fallbackSaveDirPath().toPath()
        fs.createDirectories(fallback)
        return fallback
    }

    // 抓取图片并落盘；网络/IO 异常原样上抛由调用方提示
    suspend fun saveImage(url: String, gid: Long, page: Int, fs: FileSystem = FileSystem.SYSTEM): Path = withContext(Dispatchers.IO) {
        val response = desktopGetBytes(url)
        if (response.status !in 200..299 || response.bytes.isEmpty()) {
            throw java.io.IOException("HTTP ${response.status}")
        }
        val target = uniqueTarget(saveDir(), fileNameForUrl(url, gid, page), fs)
        // 原子落盘：先写同目录 .tmp 再 rename，中断不会留下半截图片
        val temp = "$target$TEMP_SUFFIX".toPath()
        try {
            fs.write(temp) { write(response.bytes) }
            fs.atomicMove(temp, target)
        } catch (e: java.io.IOException) {
            runCatching { fs.delete(temp, mustExist = false) }
            throw e
        }
        target
    }
}
