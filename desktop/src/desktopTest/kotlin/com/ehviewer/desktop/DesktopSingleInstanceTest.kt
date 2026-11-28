package com.ehviewer.desktop

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopSingleInstanceTest {
    private fun tempLockFile(tag: String): File {
        val dir = Files.createTempDirectory("eh_lock_$tag").toFile()
        return File(dir, DesktopDirs.LOCK_FILE_NAME)
    }

    @Test
    fun secondAcquireIsRejectedWithoutLeakingHandle() {
        val lockFile = tempLockFile("same")
        try {
            assertTrue(DesktopSingleInstance.tryAcquire(lockFile))
            // 已持有时再次获取必须失败：若实现重复 open 句柄，这里会泄漏并让后续 release 失效
            assertFalse(DesktopSingleInstance.tryAcquire(lockFile))
            DesktopSingleInstance.release()
            // 释放后可重新获取（证明上面的失败没有破坏锁状态）
            assertTrue(DesktopSingleInstance.tryAcquire(lockFile))
        } finally {
            DesktopSingleInstance.release()
            lockFile.parentFile?.deleteRecursively()
        }
    }

    @Test
    fun releaseWithoutAcquireIsNoOp() {
        DesktopSingleInstance.release()
        val lockFile = tempLockFile("after-release")
        try {
            assertTrue(DesktopSingleInstance.tryAcquire(lockFile))
        } finally {
            DesktopSingleInstance.release()
            lockFile.parentFile?.deleteRecursively()
        }
    }

    @Test
    fun distinctLockFilesAreIndependent() {
        val first = tempLockFile("a")
        val second = tempLockFile("b")
        try {
            // 第二个实例（另一份数据根）不应被首个实例的锁挡住
            assertTrue(DesktopSingleInstance.tryAcquire(first))
            assertTrue(DesktopSingleInstance.tryAcquire(second))
        } finally {
            DesktopSingleInstance.release()
            DesktopSingleInstance.release()
            first.parentFile?.deleteRecursively()
            second.parentFile?.deleteRecursively()
        }
    }

    @Test
    fun lockFileFollowsDataRoot() {
        assertEquals(
            File(DesktopDirs.dataRoot, DesktopDirs.LOCK_FILE_NAME).absolutePath,
            DesktopDirs.lockFile().absolutePath,
        )
    }
}
