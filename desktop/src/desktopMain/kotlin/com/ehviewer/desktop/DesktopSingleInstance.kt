package com.ehviewer.desktop

import java.io.File
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.StandardOpenOption

// 单实例锁：对用户私有数据根下的 .lock 文件取排它文件锁（FileChannel.tryLock）。
// 不用固定 127.0.0.1:41527 端口：端口可能被无关进程占用（误判为已运行），
// 且多用户/改端口场景下互相误伤；文件锁随进程退出由 OS 回收，无残留。
object DesktopSingleInstance {
    // 按锁文件路径登记已持有的锁：同一路径重复获取直接失败（否则重复 open 会泄漏句柄），
    // 不同数据根各自独立（用于 ehviewer.data.dir 指向不同目录的多份实例）
    private val held = LinkedHashMap<String, Pair<FileChannel, FileLock>>()

    // 取锁成功 = 该数据根尚无其他实例；本进程已持有同一文件时返回 false
    fun tryAcquire(lockFile: File = DesktopDirs.lockFile()): Boolean = synchronized(this) {
        val key = lockFile.absolutePath
        if (held.containsKey(key)) return false
        try {
            lockFile.parentFile?.mkdirs()
            val opened = FileChannel.open(
                lockFile.toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
            )
            val acquired = try {
                opened.tryLock()
            } catch (e: OverlappingFileLockException) {
                null
            }
            if (acquired == null) {
                opened.close()
                return false
            }
            held[key] = opened to acquired
            true
        } catch (e: IOException) {
            false
        }
    }

    fun release() = synchronized(this) {
        held.values.forEach { (channel, lock) ->
            // 先释放锁再关 channel：顺序反了会抛 ClosedChannelException 并丢失锁状态
            try {
                lock.release()
            } catch (e: IOException) {
                // 进程退出路径上无补救动作，忽略
            }
            try {
                channel.close()
            } catch (e: IOException) {
                // 同上
            }
        }
        held.clear()
    }
}
