package com.hippo.ehviewer.updater

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApkDigestTest {
    private val sha256OfAbc = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"

    @Test
    fun `accepts sha256 prefixed digest in lower case`() {
        assertEquals(sha256OfAbc, ApkDigest.expectedSha256("sha256:${sha256OfAbc.uppercase()}"))
    }

    @Test
    fun `accepts bare digest`() {
        assertEquals(sha256OfAbc, ApkDigest.expectedSha256(sha256OfAbc))
    }

    @Test
    fun `ignores missing or unsupported digest`() {
        assertNull(ApkDigest.expectedSha256(null))
        assertNull(ApkDigest.expectedSha256(""))
        assertNull(ApkDigest.expectedSha256("md5:${"a".repeat(32)}"))
    }

    @Test
    fun `hashes a file with sha256`() {
        val file = Files.createTempFile("apk-digest", ".bin").toFile()
        try {
            file.writeText("abc")

            assertEquals(sha256OfAbc, ApkDigest.sha256(file))
        } finally {
            file.delete()
        }
    }

    @Test
    fun `verify skips when release has no digest`() {
        val file = Files.createTempFile("apk-digest", ".bin").toFile()
        try {
            file.writeText("abc")

            assertTrue(ApkDigest.verify(file, null) is DigestCheck.Skipped)
            assertTrue(ApkDigest.verify(file, "") is DigestCheck.Skipped)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `verify accepts matching digest`() {
        val file = Files.createTempFile("apk-digest", ".bin").toFile()
        try {
            file.writeText("abc")

            assertTrue(ApkDigest.verify(file, "sha256:$sha256OfAbc") is DigestCheck.Verified)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `verify reports both digests on mismatch`() {
        val file = Files.createTempFile("apk-digest", ".bin").toFile()
        try {
            file.writeText("abc")
            val expected = "0".repeat(64)

            val result = ApkDigest.verify(file, expected)

            assertTrue(result is DigestCheck.Mismatched)
            assertEquals(expected, (result as DigestCheck.Mismatched).expected)
            assertEquals(sha256OfAbc, result.actual)
        } finally {
            file.delete()
        }
    }
}
