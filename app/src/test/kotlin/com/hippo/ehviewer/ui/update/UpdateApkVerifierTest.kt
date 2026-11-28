package com.hippo.ehviewer.ui.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateApkVerifierTest {
    @Test
    fun `accepts the installed package name`() {
        assertNull(UpdateApkVerifier.checkPackageName("com.hippo.ehviewer", "com.hippo.ehviewer"))
    }

    @Test
    fun `rejects foreign or missing package name`() {
        assertNotNull(UpdateApkVerifier.checkPackageName("com.hippo.ehviewer", "com.evil.ehviewer"))
        assertNotNull(UpdateApkVerifier.checkPackageName("com.hippo.ehviewer", null))
        assertNotNull(UpdateApkVerifier.checkPackageName("com.hippo.ehviewer", ""))
    }

    @Test
    fun `accepts identical signer digests`() {
        val digest = setOf(UpdateApkVerifier.sha256(byteArrayOf(1, 2, 3)))
        assertNull(UpdateApkVerifier.checkSignerDigests(digest, digest))
    }

    @Test
    fun `rejects mismatched or empty signer digests`() {
        val installed = setOf(UpdateApkVerifier.sha256(byteArrayOf(1, 2, 3)))
        val rebuilt = setOf(UpdateApkVerifier.sha256(byteArrayOf(4, 5, 6)))
        assertNotNull(UpdateApkVerifier.checkSignerDigests(installed, rebuilt))
        assertNotNull(UpdateApkVerifier.checkSignerDigests(installed, emptySet()))
        assertNotNull(UpdateApkVerifier.checkSignerDigests(emptySet(), emptySet()))
        assertNotNull(UpdateApkVerifier.checkSignerDigests(installed, installed + rebuilt))
    }

    @Test
    fun `sha256 hashes bytes as hex`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            UpdateApkVerifier.sha256("abc".toByteArray()),
        )
    }
}
