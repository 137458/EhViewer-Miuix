/*
 * Copyright (C) 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.util

import com.ehviewer.core.files.write
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.rethrowCloseCauseIfNeeded
import java.io.File
import okio.Path

object FileUtils {
    // Workaround for filename length limit on some devices
    // https://github.com/FooIbar/EhViewer/issues/2607
    private const val MAX_FILENAME_BYTES = 200

    fun ensureDirectory(file: File?) = file?.let { if (it.exists()) it.isDirectory else it.mkdirs() } == true

    /**
     * Convert byte to human readable string.
     *
     * @param bytes the bytes to convert
     * @return the human readable string
     */
    fun humanReadableByteCount(bytes: Long): String {
        val units = "KMGTPE"
        var exp = -1
        var result = bytes.toDouble()
        while (result >= 1024.0 && exp < units.length - 1) {
            exp++
            result /= 1024.0
        }
        return if (exp == -1) "$bytes B" else "%.1f %siB".format(result, units[exp])
    }

    fun cleanupDirectory(dir: File?, maxFiles: Int = 10) {
        dir?.listFiles()?.let { files ->
            files.sortByDescending { it.lastModified() }
            files.forEachIndexed { index, file ->
                if (index >= maxFiles) file.delete()
            }
        }
    }

    private fun isValidFatFilenameChar(c: Char) = when (c.code) {
        in 0x00..0x1F,
        '"'.code,
        '*'.code,
        '/'.code,
        ':'.code,
        '<'.code,
        '>'.code,
        '?'.code,
        '\\'.code,
        '|'.code,
        0x7F,
        -> false
        else -> true
    }

    /**
     * Mutate the given filename to make it valid for a FAT filesystem,
     * replacing any invalid characters with "_".
     *
     * @param name the original filename
     */
    // From https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/android/os/FileUtils.java;l=1142;drc=7b647e4ea0e92f33c19b315eaed364ee067ba0aa
    fun sanitizeFilename(name: String): String {
        // Windows does not support filenames ending with a space or a period
        val trimmed = name.trimEnd(' ', '.')
        // 只有空格和点的名字（包括空串）清洗后什么都不剩，退回占位名而不是抛异常打断下载
        val name = trimmed.ifEmpty { "_" }
        return buildString(name.length) {
            name.forEach {
                if (isValidFatFilenameChar(it)) {
                    append(it)
                } else {
                    append('_')
                }
            }
            if (sizeInBytes > MAX_FILENAME_BYTES) {
                while (sizeInBytes > MAX_FILENAME_BYTES - 3) {
                    deleteCodePointAt(length / 2)
                }
                insert(length / 2, "...")
            }
        }
    }

    // 从中间截断时必须整码点删除，否则代理对会被劈成孤立的高/低代理字符
    private fun StringBuilder.deleteCodePointAt(index: Int) {
        if (isEmpty()) return
        val i = if (index > 0 && this[index] in '\uDC00'..'\uDFFF' && this[index - 1] in '\uD800'..'\uDBFF') index - 1 else index.coerceIn(0, length - 1)
        val count = if (this[i] in '\uD800'..'\uDBFF' && i + 1 < length && this[i + 1] in '\uDC00'..'\uDFFF') 2 else 1
        delete(i, i + count)
    }

    private val StringBuilder.sizeInBytes
        get() = toString().toByteArray().size

    /**
     * Get extension from filename
     *
     * @param filename the complete filename
     * @return null for can't find extension
     */
    fun getExtensionFromFilename(filename: String?) = filename?.substringAfterLast('.', "")?.ifEmpty { null }

    /**
     * Get name from filename
     *
     * @param filename the complete filename
     * @return null for start with . dot
     */
    fun getNameFromFilename(filename: String?) = filename?.substringBeforeLast('.')?.ifEmpty { null }
}

@OptIn(InternalAPI::class)
suspend fun ByteReadChannel.copyTo(file: Path) {
    file.write {
        while (!isClosedForRead) {
            transferFrom(readBuffer)
            awaitContent()
        }
        rethrowCloseCauseIfNeeded()
    }
}
