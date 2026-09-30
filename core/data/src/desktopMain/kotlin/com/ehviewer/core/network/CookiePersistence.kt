package com.ehviewer.core.network

import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.net.HttpCookie
import java.net.URI
import okio.FileSystem
import okio.Path

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
            ObjectOutputStream(FileOutputStream(path.toString())).use { out ->
                out.writeObject(records)
            }
        }
    }

    fun load(): Map<URI, List<HttpCookie>> {
        if (!fs.exists(path)) return emptyMap()
        return runCatching {
            ObjectInputStream(FileInputStream(path.toString())).use { input ->
                val records = input.readObject() as List<CookieRecord>
                records.groupBy({ it.uri }, { it.toHttpCookie() })
            }
        }.getOrElse {
            // 文件损坏时丢弃会话副本，等价于未登录状态，不阻塞启动
            emptyMap()
        }
    }
}
