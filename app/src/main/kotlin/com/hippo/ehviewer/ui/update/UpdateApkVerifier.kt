package com.hippo.ehviewer.ui.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File
import java.security.MessageDigest

/**
 * 安装前的更新包身份校验。
 *
 * 只信任"同包名 + 同签名证书"的安装包，避免第三方重签包或降级替换包被静默安装。
 * 失败原因以可读字符串返回，由调用方转成可见的失败提示，不做静默吞掉。
 */
internal object UpdateApkVerifier {
    fun verify(context: Context, file: File): String? {
        if (!file.isFile) return "update package missing: ${file.name}"
        if (file.length() == 0L) return "update package is empty: ${file.name}"
        val pm = context.packageManager
        val flags = packageInfoFlags()
        val archive = pm.getPackageArchiveInfo(file.absolutePath, flags)
            ?: return "cannot parse update package: ${file.name}"
        checkPackageName(context.packageName, archive.packageName)?.let { return it }
        val installed = runCatching { pm.getPackageInfo(context.packageName, flags) }.getOrNull()
            ?: return "cannot read installed package: ${context.packageName}"
        return checkSignerDigests(signerDigests(installed), signerDigests(archive))
    }

    fun checkPackageName(expected: String, actual: String?): String? = when {
        actual.isNullOrEmpty() -> "update package has no package name"
        actual != expected -> "update package name $actual does not match $expected"
        else -> null
    }

    fun checkSignerDigests(installed: Set<String>, incoming: Set<String>): String? = when {
        installed.isEmpty() -> "installed signer digest unavailable"
        incoming.isEmpty() -> "update package signer digest unavailable"
        installed != incoming -> "update package signature does not match the installed one"
        else -> null
    }

    fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return buildString(digest.size * 2) {
            digest.forEach { append((it.toInt() and 0xff).toString(16).padStart(2, '0')) }
        }
    }

    private fun signerDigests(info: PackageInfo): Set<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        readSigningDigests(info)
    } else {
        readLegacySignatures(info).mapTo(mutableSetOf()) { sha256(it.toByteArray()) }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun readSigningDigests(info: PackageInfo): Set<String> {
        val signingInfo = info.signingInfo ?: return emptySet()
        val signatures = if (signingInfo.hasMultipleSigners()) {
            signingInfo.apkContentsSigners
        } else {
            signingInfo.signingCertificateHistory
        }
        return signatures.mapTo(mutableSetOf()) { sha256(it.toByteArray()) }
    }

    @Suppress("DEPRECATION")
    private fun readLegacySignatures(info: PackageInfo): Array<Signature> = info.signatures ?: emptyArray()

    @Suppress("DEPRECATION")
    private fun packageInfoFlags(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PackageManager.GET_SIGNING_CERTIFICATES
    } else {
        PackageManager.GET_SIGNATURES
    }
}
