package com.ehviewer.core.util

import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path
import okio.Path.Companion.toPath

// DesktopFileLog 的并发与路径鲁棒性：临界区互斥、无父目录时轮转、日志根目录推导
class DesktopFileLogHardeningTest {
    // 把「无父目录」的相对路径映射进临时目录，既模拟 file.parent == null 又不写脏工作目录
    private class SandboxFileSystem(private val root: Path) : ForwardingFileSystem(FileSystem.SYSTEM) {
        override fun onPathParameter(path: Path, functionName: String, parameterName: String): Path = root / path.name
    }

    @Test
    fun logLockSerializesCriticalSections() {
        val threads = 4
        val barrier = CyclicBarrier(threads)
        val inFlight = AtomicInteger()
        val maxInFlight = AtomicInteger()
        val workers = (1..threads).map {
            Thread {
                barrier.await()
                repeat(50) {
                    withLogLock {
                        val concurrent = inFlight.incrementAndGet()
                        maxInFlight.updateAndGet { current -> maxOf(current, concurrent) }
                        inFlight.decrementAndGet()
                    }
                }
            }
        }
        workers.forEach(Thread::start)
        workers.forEach(Thread::join)
        assertEquals(1, maxInFlight.get(), "append/rotate 临界区必须互斥")
    }

    @Test
    fun rolledLogPathHandlesMissingParent() {
        // 根路径（"/"、".", "C:\\")的 parent 为 null，轮转名必须退回同目录相对名而不是 NPE
        assertEquals("ehviewer.log.1".toPath(), rolledLogPath(null, "ehviewer.log"))
        assertEquals("/logs/ehviewer.log.1".toPath(), rolledLogPath("/logs".toPath(), "ehviewer.log"))
    }

    @Test
    fun rotatesRelativePathKeepingOneGeneration() {
        val root = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "eh_filelog_relative_${System.nanoTime()}"
        FileSystem.SYSTEM.createDirectories(root)
        try {
            val fs = SandboxFileSystem(root)
            val file = "ehviewer.log".toPath()
            fs.write(file) { writeUtf8("x".repeat(DesktopFileLog.MAX_BYTES.toInt())) }
            DesktopFileLog.append(file, "new", fs)
            assertTrue(fs.exists(root / "ehviewer.log.1"), "轮转文件应落在同目录")
            assertEquals("new\n", fs.read(file) { readUtf8() })
        } finally {
            FileSystem.SYSTEM.deleteRecursively(root)
        }
    }

    @Test
    fun resolvesLogRootDirInPriorityOrder() {
        assertEquals("D:/data", resolveLogRootDir("D:\\data", "C:\\AppData", "/state", "/home/u"))
        assertEquals("C:/AppData", resolveLogRootDir(null, "C:\\AppData", "/state", "/home/u"))
        assertEquals("/state", resolveLogRootDir("  ", null, "/state", "/home/u"))
        assertEquals("/home/u/.ehviewer", resolveLogRootDir(null, null, null, "/home/u"))
        assertNull(resolveLogRootDir(null, null, null, null))
    }

    @Test
    fun trimsTrailingSeparatorsFromRootDir() {
        assertEquals("/data", resolveLogRootDir("/data/", null, null, "/home/u"))
        assertEquals("D:/data", resolveLogRootDir("D:\\data\\", null, null, null))
    }
}
