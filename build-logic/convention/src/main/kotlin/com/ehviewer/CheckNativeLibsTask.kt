package com.ehviewer

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * 校验入库的预编译原生库：文件存在，且 Kotlin 侧声明的每个 `external fun` 都能在
 * `.so` 的动态符号表里找到对应的 `Java_*` 导出。缺库或缺符号都让构建直接失败，
 * 而不是等到 System.loadLibrary 抛 UnsatisfiedLinkError 才暴露。
 *
 * 定义在 build-logic 而不是 build.gradle.kts：Kotlin DSL 脚本里声明的 class 会成为
 * 脚本类的非静态内部类，Gradle 无法实例化成 Task 类型。
 */
abstract class CheckNativeLibsTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val nativeLibs: ConfigurableFileCollection

    @get:Input
    abstract val requiredJniSymbols: ListProperty<String>

    @TaskAction
    fun verify() {
        val missing = nativeLibs.files.filterNot { it.isFile }
        if (missing.isNotEmpty()) {
            throw GradleException(
                "缺少原生库 libehviewer.so（ABI: ${missing.map { it.parentFile.name }}），此 APK 可编译但启动即崩。\n" +
                    "  1) 带 -PbuildNative 执行原生构建（Windows 主机需 WSL，见 app/src/main/cpp/CMakeLists.txt）；\n" +
                    "  2) 或将预编译的 libehviewer.so 放入 app/src/main/jniLibs/<abi>/；\n" +
                    "  仅编译验证 UI 可追加 -PskipNativeCheck 豁免本校验。",
            )
        }
        val required = requiredJniSymbols.get()
        val failed = nativeLibs.files.filter { it.isFile }.mapNotNull { library ->
            val exported = definedDynamicSymbols(library)
            val absent = required.filterNot { it in exported }
            if (absent.isEmpty()) null else "$library 缺少 JNI 导出符号：${absent.joinToString()}"
        }
        if (failed.isNotEmpty()) {
            throw GradleException(
                failed.joinToString("\n") + "\n" +
                    "  入库的 libehviewer.so 与 Kotlin 侧 external fun 声明脱钩，运行到对应调用会抛 UnsatisfiedLinkError。\n" +
                    "  带 -PbuildNative 重编原生库并同步提交新 .so；仅编译验证可追加 -PskipNativeCheck 豁免本校验。",
            )
        }
    }
}

// 解析 ELF64 小端共享库的 .dynsym，返回到已定义（st_shndx != SHN_UNDEF）符号名集合。
// 只需支持 arm64-v8a / x86_64 的 Android 产物，因此不做 32 位与大小端分支。
private fun definedDynamicSymbols(file: File): Set<String> = runCatching {
    val bytes = file.readBytes()
    fun u8(offset: Int) = bytes[offset].toInt() and 0xff
    fun u16(offset: Int) = u8(offset) or (u8(offset + 1) shl 8)
    fun u32(offset: Int) = u16(offset) or (u16(offset + 2) shl 16)
    fun u64(offset: Int) = u32(offset).toLong() and 0xffffffffL or (u32(offset + 4).toLong() shl 32)
    require(u8(0) == 0x7f && u8(1) == 'E'.code && u8(2) == 'L'.code && u8(3) == 'F'.code) { "不是 ELF 文件" }
    require(u8(4) == 2 && u8(5) == 1) { "只支持 ELF64 小端" }
    val sectionOffset = u64(0x28).toInt()
    val sectionEntrySize = u16(0x3a)
    val sectionCount = u16(0x3c)
    val symbols = LinkedHashSet<String>()
    for (index in 0 until sectionCount) {
        val header = sectionOffset + index * sectionEntrySize
        if (u32(header + 4) != 11) continue // SHT_DYNSYM
        val symbolsOffset = u64(header + 0x18).toInt()
        val symbolsSize = u64(header + 0x20).toInt()
        val stringsOffset = u64(sectionOffset + u32(header + 0x28) * sectionEntrySize + 0x18).toInt()
        for (offset in symbolsOffset until symbolsOffset + symbolsSize step 24) {
            val nameOffset = u32(offset)
            if (nameOffset == 0 || u16(offset + 6) == 0) continue // 空名或 SHN_UNDEF（未定义的外部引用）
            val start = stringsOffset + nameOffset
            val end = (start until bytes.size).firstOrNull { bytes[it] == 0.toByte() } ?: bytes.size
            symbols += String(bytes, start, end - start, Charsets.UTF_8)
        }
    }
    symbols
}.getOrElse { emptySet() }
