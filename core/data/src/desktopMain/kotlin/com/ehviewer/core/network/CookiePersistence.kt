package com.ehviewer.core.network

import com.sun.jna.Platform
import com.sun.jna.platform.win32.Crypt32Util
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.net.HttpCookie
import java.net.URI
import okio.FileSystem
import okio.Path
import okio.buffer

// HttpCookie 不可序列化，落盘用自有 DTO；会话 cookie 重启后转为持久，保证桌面端登录态延续
internal data class CookieRecord(
    val uri: URI,
    val name: String,
    val value: String,
    val domain: String?,
    val path: String?,
    val secure: Boolean,
) : Serializable {
    fun toHttpCookie() = HttpCookie(name, value).apply {
        domain = this@CookieRecord.domain
        path = this@CookieRecord.path?.ifEmpty { "/" } ?: "/"
        setSecure(secure)
    }

    companion object {
        fun from(uri: URI, cookie: HttpCookie) = CookieRecord(
            uri = uri,
            name = cookie.getName(),
            value = cookie.getValue(),
            domain = cookie.getDomain(),
            path = cookie.getPath(),
            secure = cookie.getSecure(),
        )
    }
}

// 独立持久化单元：save/load 与内存态解耦，load 可在新进程/新实例上验证恢复语义
internal class CookiePersistence(private val path: Path) {
    private val fs = FileSystem.SYSTEM

    fun save(entries: Map<URI, List<HttpCookie>>) {
        runCatching {
            path.parent?.let { fs.createDirectories(it) }
            val records = entries.flatMap { (uri, cookies) ->
                cookies.map { c -> CookieRecord.from(uri, c) }
            }
            val plain = ByteArrayOutputStream().also { buffer ->
                ObjectOutputStream(buffer).use { it.writeObject(records) }
            }.toByteArray()
            // DPAPI CurrentUser 域加密：文件被拷走后无法在他人账户解出登录态。
            // Crypt32 仅 Windows 可用；非 Windows 桌面（分发目标仅 MSI/Exe）退回明文，
            // 保证 Linux/macOS 构建与 CI 的明文回环可用
            val protected = if (Platform.isWindows()) Crypt32Util.cryptProtectData(plain) else plain
            fs.sink(path).buffer().use { it.write(protected) }
        }
    }

    fun load(): Map<URI, List<HttpCookie>> {
        if (!fs.exists(path)) return emptyMap()
        val raw = runCatching { fs.source(path).buffer().use { it.readByteArray() } }.getOrElse { return emptyMap() }
        // 优先按 DPAPI 解密；旧版明文格式直接反序列化（一次性迁移）；损坏则回退未登录
        return runCatching { deserialize(Crypt32Util.cryptUnprotectData(raw)) }
            .recoverCatching { deserialize(raw) }
            .getOrElse { emptyMap() }
    }

    private fun deserialize(bytes: ByteArray): Map<URI, List<HttpCookie>> = ObjectInputStream(ByteArrayInputStream(bytes)).use { input ->
        val records = input.readObject() as List<CookieRecord>
        records.groupBy({ it.uri }, { it.toHttpCookie() })
    }
}
