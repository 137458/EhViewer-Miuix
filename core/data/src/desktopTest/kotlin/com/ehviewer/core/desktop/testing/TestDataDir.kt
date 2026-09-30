package com.ehviewer.core.desktop.testing

import java.nio.file.Files
import okio.Path
import okio.Path.Companion.toPath

// 将桌面数据根目录重定向到一次性临时目录（ehviewer.data.dir 系统属性），
// 保证 desktopTest 不读写真实用户数据 %APPDATA%/EhViewer。
// 返回值统一正斜杠，避免与 okio.Path 字符串比较时因分隔符不一致而失配。
fun newIsolatedDataDir(): Path {
    val dir = Files.createTempDirectory("ehviewer_desktop_test").toAbsolutePath().toString().replace('\\', '/')
    System.setProperty("ehviewer.data.dir", dir)
    return dir.toPath()
}

fun clearIsolatedDataDir() {
    System.clearProperty("ehviewer.data.dir")
}
