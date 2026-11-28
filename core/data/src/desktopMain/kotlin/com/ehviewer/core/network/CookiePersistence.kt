package com.ehviewer.core.network

import com.sun.jna.Platform
import com.sun.jna.platform.win32.Crypt32Util
import java.io.ByteArrayInputStream
import java.io.ObjectInputFilter
import java.io.ObjectInputStream
import java.io.Serializable
import java.net.HttpCookie
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions
import kotlinx.serialization.Serializable as JsonSerializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer

// 旧版落盘 DTO：仅保留用于读取历史文件（类体不可改动，否则默认 serialVersionUID 变化会让旧流反序列化失败）
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

// 现行落盘 DTO：JSON 文本，无 Java 反序列化攻击面，可直接承载绝对过期时间
@JsonSerializable
internal data class CookieJsonRecord(
    val name: String,
    val value: String,
    val domain: String,
    val path: String,
    val secure: Boolean = false,
    val hostOnly: Boolean = true,
    val expiresAtEpochMs: Long? = null,
) {
    fun isExpired(nowMs: Long): Boolean = expiresAtEpochMs != null && expiresAtEpochMs <= nowMs

    fun toHttpCookie(nowMs: Long) = HttpCookie(name, value).apply {
        domain = this@CookieJsonRecord.domain
        path = this@CookieJsonRecord.path
        setSecure(secure)
        maxAge = expiresAtEpochMs?.let { ((it - nowMs) / 1000).coerceAtLeast(0) } ?: -1L
    }
}

// 独立持久化单元：save/load 与内存态解耦，load 可在新进程/新实例上验证恢复语义
internal class CookiePersistence(private val path: Path) {
    private val fs = FileSystem.SYSTEM
    private val json = Json { ignoreUnknownKeys = true }
    private val recordsSerializer = ListSerializer(CookieJsonRecord.serializer())

    fun save(records: List<CookieJsonRecord>) {
        runCatching {
            path.parent?.let { fs.createDirectories(it) }
            val plain = json.encodeToString(recordsSerializer, records).encodeToByteArray()
            // DPAPI CurrentUser 域加密：文件被拷走后无法在他人账户解出登录态。
            // Crypt32 仅 Windows 可用；非 Windows 桌面（分发目标仅 MSI/Exe）退回明文，
            // 与既有行为一致，仅把格式换成 JSON
            val protected = if (Platform.isWindows()) Crypt32Util.cryptProtectData(plain) else plain
            // 先写临时文件再原子替换：崩溃/掉电不会留下半截 cookie 存储
            val tempName = "${path.name}.${System.nanoTime()}.tmp"
            val temp = path.parent?.let { it / tempName } ?: tempName.toPath()
            fs.sink(temp).buffer().use { it.write(protected) }
            restrictToOwner(temp)
            runCatching { fs.atomicMove(temp, path) }.onFailure {
                fs.delete(path)
                fs.atomicMove(temp, path)
            }
        }
    }

    fun load(): List<CookieJsonRecord> {
        if (!fs.exists(path)) return emptyList()
        val raw = runCatching { fs.source(path).buffer().use { it.readByteArray() } }.getOrElse { return emptyList() }
        if (raw.isEmpty()) return emptyList()
        // Windows 只接受 DPAPI 密文：解不开即视为损坏，绝不回退明文反序列化，
        // 否则任何人都能投放一段序列化流注入可控 cookie（无用户账户绑定）
        val plain = if (Platform.isWindows()) {
            runCatching { Crypt32Util.cryptUnprotectData(raw) }.getOrElse { return emptyList() }
        } else {
            raw
        }
        // 旧版 Java 序列化格式：一次性迁移到 JSON，并立即改写为加密 JSON 落盘
        if (isJavaSerializationStream(plain)) {
            val migrated = runCatching { deserializeLegacy(plain) }.getOrElse { return emptyList() }
            save(migrated)
            return migrated
        }
        return runCatching { json.decodeFromString(recordsSerializer, plain.decodeToString()) }.getOrElse { emptyList() }
    }

    private fun isJavaSerializationStream(bytes: ByteArray): Boolean = bytes.size >= 2 && bytes[0] == 0xAC.toByte() && bytes[1] == 0xED.toByte()

    // 严格输入过滤器：字符串白名单只列旧流里可能出现的类，其余一律拒绝
    private val legacyClassFilter = ObjectInputFilter.Config.createFilter(
        "com.ehviewer.core.network.CookieRecord;java.net.URI;java.util.ArrayList;java.lang.String;java.lang.Boolean;maxdepth=5;maxrefs=10000;maxarray=10000;!*",
    )
    private val legacyArrayComponentTypes = setOf(Any::class.java, String::class.java)

    // 数组本身不带可执行代码，且过滤器的字符串语法无法表达 "[Ljava.lang.Object;"（; 是分隔符）：
    // 数组按元素类型单独放行（ArrayList 的序列化流里会出现 Object[]），元素自身仍逐个走白名单
    private val legacyFilter = ObjectInputFilter { info ->
        when (val decision = legacyClassFilter.checkInput(info)) {
            ObjectInputFilter.Status.REJECTED -> {
                val type = info.serialClass()
                if (type != null && type.isArray && type.componentType in legacyArrayComponentTypes) {
                    ObjectInputFilter.Status.UNDECIDED
                } else {
                    decision
                }
            }
            else -> decision
        }
    }

    private fun deserializeLegacy(bytes: ByteArray): List<CookieJsonRecord> = ObjectInputStream(ByteArrayInputStream(bytes)).use { input ->
        input.setObjectInputFilter(legacyFilter)
        (input.readObject() as List<*>).filterIsInstance<CookieRecord>().map { it.toJsonRecord() }
    }

    private fun restrictToOwner(target: Path) {
        if (Platform.isWindows()) return
        // 非 Windows 明文落盘：收紧到仅属主可读写
        runCatching { Files.setPosixFilePermissions(Paths.get(target.toString()), PosixFilePermissions.fromString("rw-------")) }
    }
}

private fun CookieRecord.toJsonRecord(): CookieJsonRecord {
    val host = uri.host?.lowercase().orEmpty()
    val normalizedDomain = normalizeCookieDomain(domain) ?: host
    return CookieJsonRecord(
        name = name,
        value = value,
        domain = normalizedDomain,
        path = normalizeCookiePath(path),
        secure = secure,
        // 旧记录无法区分「未带域属性」与「域属性等于 host」：只有带前导点的原样值可判定为域 cookie，
        // 其余按 host-only 处理（收紧，不放宽）
        hostOnly = domain?.startsWith(".") != true,
        expiresAtEpochMs = null,
    )
}
