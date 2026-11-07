package com.hippo.ehviewer.updater

import java.io.File
import java.security.MessageDigest

internal sealed interface DigestCheck {
    data object Skipped : DigestCheck

    data object Verified : DigestCheck

    data class Mismatched(val expected: String, val actual: String) : DigestCheck
}

internal object ApkDigest {
    private val SHA256_PATTERN = Regex("""(?:sha256:)?([0-9a-fA-F]{64})""")

    /**
     * GitHub asset digest 形如 `sha256:<hex>`；没有可解析的 SHA-256 时返回 null，
     * 调用方应跳过校验而不是把下载判为失败。
     */
    fun expectedSha256(digest: String?): String? {
        val value = digest?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return SHA256_PATTERN.matchEntire(value)?.groupValues?.get(1)?.lowercase()
    }

    fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                md.update(buffer, 0, read)
            }
        }
        return buildString(32) {
            md.digest().forEach { byte -> append("%02x".format(byte.toInt() and 0xFF)) }
        }
    }

    fun verify(file: File, digest: String?): DigestCheck {
        val expected = expectedSha256(digest) ?: return DigestCheck.Skipped
        val actual = sha256(file)
        return if (expected == actual) DigestCheck.Verified else DigestCheck.Mismatched(expected, actual)
    }
}
