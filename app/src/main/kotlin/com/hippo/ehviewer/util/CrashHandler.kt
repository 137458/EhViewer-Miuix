package com.hippo.ehviewer.util

import android.os.Build
import android.os.Debug
import com.ehviewer.core.util.logcat
import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.Settings
import java.io.File
import java.io.PrintWriter
import java.io.Writer
import java.lang.reflect.Modifier

private fun joinIfStringArray(any: Any?): String = if (any is Array<*>) any.joinToString() else any.toString()

// 只读静态字段：实例字段用 get(null) 读会抛 NullPointerException，整份崩溃日志就写不出来了
private fun collectClassStaticInfo(clazz: Class<*>): String = clazz.fields.filter { Modifier.isStatic(it.modifiers) }.joinToString("\n") {
    "${it.name}=${joinIfStringArray(it.get(null))}"
}

object CrashHandler {
    fun install() {
        val handler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            if (Settings.saveCrashLog.value) {
                runCatching { saveCrashLog(e) }
            }
            handler?.uncaughtException(t, e)
        }
    }

    fun collectInfo(writer: Writer) {
        writer.write("======== PackageInfo ========\n")
        writer.write("PackageName=${BuildConfig.APPLICATION_ID}\n")
        writer.write("VersionName=${BuildConfig.VERSION_NAME}\n")
        writer.write("VersionCode=${BuildConfig.VERSION_CODE}\n")
        writer.write("CommitSha=${BuildConfig.COMMIT_SHA}\n")
        writer.write("CommitTime=${AppConfig.commitTime}\n")
        writer.write("\n")

        // Device info
        writer.write("======== DeviceInfo ========\n")
        writer.write("${collectClassStaticInfo(Build::class.java)}\n")
        writer.write("${collectClassStaticInfo(Build.VERSION::class.java)}\n")
        writer.write("MEMORY=")
        writer.write(FileUtils.humanReadableByteCount(OSUtils.appAllocatedMemory))
        writer.write("\n")
        writer.write("MEMORY_NATIVE=")
        writer.write(FileUtils.humanReadableByteCount(Debug.getNativeHeapAllocatedSize()))
        writer.write("\n")
        writer.write("MEMORY_MAX=")
        writer.write(FileUtils.humanReadableByteCount(OSUtils.appMaxMemory))
        writer.write("\n")
        writer.write("MEMORY_TOTAL=")
        writer.write(FileUtils.humanReadableByteCount(OSUtils.totalMemory))
        writer.write("\n")
        writer.write("\n")

        writer.flush()
    }

    private fun getThrowableInfo(t: Throwable, writer: PrintWriter) {
        // printStackTrace 自己就会把 Caused by 链打全，再手动遍历会重复整条链
        t.printStackTrace(writer)
    }

    private fun saveCrashLog(t: Throwable) {
        val dir = AppConfig.externalCrashDir ?: return
        val nowString = ReadableTime.getFilenamableTime()
        val fileName = "crash-$nowString.log"
        val file = File(dir, fileName)
        runCatching {
            file.printWriter().use { writer ->
                writer.write("TIME=${nowString}\n")
                writer.write("\n")
                collectInfo(writer)
                writer.write("======== CrashInfo ========\n")
                getThrowableInfo(t, writer)
                writer.write("\n")
            }
        }.onFailure {
            logcat(it)
            file.delete()
        }
        if (t is OutOfMemoryError) {
            val dumpFile = File(dir, "hprof-$nowString.hprof")
            Debug.dumpHprofData(dumpFile.path)
        }
    }
}
