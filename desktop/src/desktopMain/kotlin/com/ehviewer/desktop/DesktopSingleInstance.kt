package com.ehviewer.desktop

import java.net.InetAddress
import java.net.ServerSocket

// 单实例锁：绑定 127.0.0.1 固定端口，二次启动获取失败即退出。
// 进程死亡时 OS 回收套接字，无残留锁文件。
object DesktopSingleInstance {
    private var serverSocket: ServerSocket? = null

    // 绑定成功 = 获得单实例锁；端口已被占（他人或自己已持有）返回 false
    fun tryAcquire(port: Int = DEFAULT_PORT): Boolean = runCatching {
        ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"))
    }.onSuccess { serverSocket = it }.isSuccess

    fun release() {
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    const val DEFAULT_PORT = 41_527
}
