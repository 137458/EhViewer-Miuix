package com.ehviewer.core.files

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import okio.Path.Companion.toPath

// SAF 树文档 URI 的路径段重建、子项名校验与同父目录判定（AndroidFileSystem/Paths.android 的纯逻辑部分）
class DocumentPathTest {
    // 与 android.net.Uri.decode 等价的百分号解码（含 UTF-8 多字节）
    private fun decode(input: String): String {
        if ('%' !in input) return input
        val out = ArrayList<Byte>(input.length)
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c == '%' && i + 2 < input.length) {
                out += input.substring(i + 1, i + 3).toInt(16).toByte()
                i += 3
            } else {
                out += c.toString().encodeToByteArray().toList()
                i++
            }
        }
        return out.toByteArray().decodeToString()
    }

    private fun rebuild(segments: List<String>) = rebuildTreeDocumentId(segments) { decode(it) }

    @Test
    fun rebuildsTreeDocumentIdWithSingleDecode() {
        val segments = listOf("tree", "primary%3ADocs", "document", "primary%3ADocs", "sub", "a%23b%3Fc.txt")
        assertEquals("primary:Docs/sub/a#b?c.txt", rebuild(segments))
    }

    @Test
    fun keepsVolumeRootWithoutExtraSlash() {
        assertEquals(
            "primary:sub/a.txt",
            rebuild(listOf("tree", "primary%3A", "document", "primary%3A", "sub", "a.txt")),
        )
    }

    @Test
    fun decodesNonAsciiSegmentsOnce() {
        assertEquals(
            "primary:Docs/中.txt",
            rebuild(listOf("tree", "primary%3ADocs", "document", "primary%3ADocs", "%E4%B8%AD.txt")),
        )
    }

    @Test
    fun returnsNullForNonTreeUri() {
        assertNull(rebuild(listOf("document", "primary%3ADocs", "a", "b", "c")))
    }

    @Test
    fun returnsNullForTreeUriWithoutChildSegments() {
        assertNull(rebuild(listOf("tree", "primary%3ADocs", "document", "primary%3ADocs")))
    }

    @Test
    fun rejectsUnsafeChildNames() {
        assertFalse(isSafeChildName(""))
        assertFalse(isSafeChildName("."))
        assertFalse(isSafeChildName(".."))
        assertFalse(isSafeChildName("a/b"))
        assertFalse(isSafeChildName("/abs"))
        assertFalse(isSafeChildName("../escape"))
    }

    @Test
    fun acceptsOrdinaryChildNames() {
        assertTrue(isSafeChildName("file.txt"))
        assertTrue(isSafeChildName("a..b.txt"))
        assertTrue(isSafeChildName("中文名.png"))
    }

    @Test
    fun detectsSameParentDirectory() {
        assertTrue(
            isSameParentDirectory(
                "/sdcard/Docs/a.txt".toPath(),
                "/sdcard/Docs/b.txt".toPath(),
            ),
        )
        assertFalse(
            isSameParentDirectory(
                "/sdcard/Docs/a.txt".toPath(),
                "/sdcard/Other/b.txt".toPath(),
            ),
        )
        assertTrue(
            isSameParentDirectory(
                "content://x/tree/a/document/a/sub/1.txt".toPath(),
                "content://x/tree/a/document/a/sub/2.txt".toPath(),
            ),
        )
        assertFalse(
            isSameParentDirectory(
                "content://x/tree/a/document/a/sub/1.txt".toPath(),
                "content://x/tree/a/document/a/other/1.txt".toPath(),
            ),
        )
    }
}
