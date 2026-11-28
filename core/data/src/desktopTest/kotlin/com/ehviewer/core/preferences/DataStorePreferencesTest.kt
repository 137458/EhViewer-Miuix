package com.ehviewer.core.preferences

import com.ehviewer.core.desktop.testing.clearIsolatedDataDir
import com.ehviewer.core.desktop.testing.newIsolatedDataDir
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

private class TestPreferences(name: String) : DataStorePreferences(name) {
    val sample = stringPref("sample", "default")
}

// 偏好层：损坏文件降级、写入可见性、异步写入顺序、快照同步
class DataStorePreferencesTest {
    private val tag = "pref_test_${System.nanoTime()}"

    @BeforeTest
    fun setUp() {
        newIsolatedDataDir()
    }

    @AfterTest
    fun tearDown() {
        clearIsolatedDataDir()
    }

    @Test
    fun corruptedStoreDegradesToDefaultsInsteadOfThrowing(): Unit = runBlocking {
        val name = "${tag}_corrupt"
        val path = preferencesPath(name)
        Files.createDirectories(Paths.get(path.parent.toString()))
        // 非 Preferences 序列化格式的字节：解析必然失败，DataStore 会抛 CorruptionException
        Files.write(Paths.get(path.toString()), byteArrayOf(-1, -2, -3, -4, -5, -6, -7, -8))

        val preferences = TestPreferences(name)
        assertEquals("default", preferences.sample.value, "损坏的偏好文件必须降级为默认值")

        // 损坏文件被替换后仍然可用
        preferences.sample.value = "after-corruption"
        withTimeout(5_000) {
            preferences.data.first { it[preferences.sample] == "after-corruption" }
        }
    }

    @Test
    fun writeIsVisibleImmediately(): Unit = runBlocking {
        val preferences = TestPreferences("${tag}_write")
        preferences.sample.value = "first"
        assertEquals("first", preferences.sample.value)
    }

    @Test
    fun coldReadDoesNotPinStaleValueAfterExternalWrite(): Unit = runBlocking {
        val preferences = TestPreferences("${tag}_cold")
        // 冷读：此时快照还未就绪，走 runBlocking 分支；该次读取不得进入本地缓存
        assertEquals("default", preferences.sample.value)

        // 绕过 PrefDelegate 写入（模拟其它来源/其它实例对同一偏好的修改）
        preferences.withMutableSnapshot { it[preferences.sample] = "external" }
        withTimeout(5_000) {
            preferences.data.first { it[preferences.sample] == "external" }
        }

        assertEquals("external", preferences.sample.value, "冷读不得把默认值永久钉在本地缓存里")
    }

    @Test
    fun asyncWritesApplyInSubmitOrder(): Unit = runBlocking {
        val preferences = TestPreferences("${tag}_order")
        repeat(100) { preferences.sample.value = "v$it" }
        withTimeout(5_000) {
            preferences.data.first { it[preferences.sample] == "v99" }
        }
        assertEquals("v99", preferences.sample.value, "异步写入必须按提交顺序落盘")
    }

    @Test
    fun mutableSnapshotWriteUpdatesSnapshotCacheSynchronously(): Unit = runBlocking {
        val preferences = TestPreferences("${tag}_snapshot")
        preferences.snapshot()
        preferences.withMutableSnapshot { it[preferences.sample] = "direct" }
        assertEquals(
            "direct",
            preferences.currentSnapshot()?.get(preferences.sample),
            "edit 返回后快照缓存必须已包含本次写入",
        )
    }
}

// 与 TestPreferences 共享同一偏好的另一个 DataStorePreferences 实例会拿到同一 DataStore 单例，
// 这里仅用于验证跨实例可见性
private class TestPreferencesWithoutCollector(name: String) : DataStorePreferences(name) {
    val sample = stringPref("sample", "default")
}
