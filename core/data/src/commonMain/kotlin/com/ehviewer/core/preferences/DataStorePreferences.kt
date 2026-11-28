package com.ehviewer.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.ehviewer.core.util.logcat
import kotlin.concurrent.Volatile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal expect fun getDateStore(name: String?): DataStore<Preferences>

abstract class DataStorePreferences(private val dataStore: DataStore<Preferences>) {
    constructor(name: String?) : this(getDateStore(name))

    @Volatile
    private var cachedSnapshot: Snapshot? = null

    init {
        prefScope.launch {
            dataStore.data
                // 偏好文件损坏或不可读时 DataStore 会抛异常。收集器一旦崩掉，cachedSnapshot
                // 会永远停留在旧值，之后所有读取都会拿到过期数据。
                .catch {
                    // 收集器不能就此停摆：否则 cachedSnapshot 会永远停在旧值。
                    // 以空偏好继续，读取退化为默认值
                    logcat("DataStorePreferences", it)
                    emit(emptyPreferences())
                }
                .collect {
                    cachedSnapshot = Snapshot(it)
                }
        }
    }

    val data: Flow<Snapshot>
        get() = dataStore.data.map { Snapshot(it) }

    suspend fun snapshot(): Snapshot = cachedSnapshot ?: data.first().also { cachedSnapshot = it }

    fun currentSnapshot(): Snapshot? = cachedSnapshot

    suspend fun withMutableSnapshot(block: (MutableSnapshot) -> Unit) {
        val updated = dataStore.edit {
            block(MutableSnapshot(it))
        }
        // edit 返回的就是本次写入后的完整状态：立即刷新快照缓存，
        // 消除「写入已完成、currentSnapshot() 仍是旧值」的观察窗口
        cachedSnapshot = Snapshot(updated)
    }

    protected fun boolPref(
        key: String,
        defaultValue: Boolean,
    ) = PrefDelegate(this, booleanPreferencesKey(name = key), defaultValue = defaultValue)

    protected fun intPref(
        key: String,
        defaultValue: Int,
    ) = PrefDelegate(this, intPreferencesKey(name = key), defaultValue = defaultValue)

    protected fun floatPref(
        key: String,
        defaultValue: Float,
    ) = PrefDelegate(this, floatPreferencesKey(name = key), defaultValue = defaultValue)

    protected fun doublePref(
        key: String,
        defaultValue: Double,
    ) = PrefDelegate(this, doublePreferencesKey(name = key), defaultValue = defaultValue)

    protected fun longPref(
        key: String,
        defaultValue: Long,
    ) = PrefDelegate(this, longPreferencesKey(name = key), defaultValue = defaultValue)

    protected fun stringPref(
        key: String,
        defaultValue: String,
    ) = PrefDelegate(this, stringPreferencesKey(name = key), defaultValue = defaultValue)

    protected fun stringOrNullPref(
        key: String,
    ) = PrefDelegate(this, stringPreferencesKey(name = key), null)

    protected fun stringSetPref(
        key: String,
        defaultValue: Set<String> = emptySet(),
    ) = PrefDelegate(this, stringSetPreferencesKey(name = key), defaultValue = defaultValue)

    protected fun stringSetOrNullPref(
        key: String,
    ) = PrefDelegate(this, stringSetPreferencesKey(name = key), null)

    protected fun byteArrayPref(
        key: String,
        defaultValue: ByteArray,
    ) = PrefDelegate(this, byteArrayPreferencesKey(name = key), defaultValue = defaultValue)
}
