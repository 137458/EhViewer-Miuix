package com.ehviewer.core.preferences

import androidx.datastore.preferences.core.Preferences
import kotlin.concurrent.atomics.AtomicReference
import kotlin.reflect.KProperty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

// 尚未被快照确认的本地写入：值与「是否存在」必须一起原子发布，
// 否则读线程可能看到「有缓存」却读到已被清空的值
private class PendingValue<T>(val value: T)

@Suppress("NOTHING_TO_INLINE")
class PrefDelegate<T> internal constructor(
    private val preferences: DataStorePreferences,
    val key: Preferences.Key<T & Any>,
    val defaultValue: T,
) {
    private val pendingValue = AtomicReference<PendingValue<T>?>(null)

    fun changesFlow(): Flow<Unit> = valueFlow().drop(1).map {}

    fun valueFlow(): Flow<T> = preferences.data.map { it[this] }.distinctUntilChanged()

    var value: T
        @Suppress("UNCHECKED_CAST")
        get() {
            // 写入是异步的，快照会晚一拍才更新。本地写入必须优先于快照，否则同帧内
            // 「写入后立刻读取」仍然会读到旧值；等快照追上后再丢弃缓存，保证之后能读到外部变更。
            val pending = pendingValue.load()
            if (pending != null) {
                if (preferences.currentSnapshot()?.get(this) == pending.value) {
                    pendingValue.compareAndSet(pending, null)
                } else {
                    return pending.value
                }
            }
            // 冷读不得写入缓存：否则快照就绪前读到的默认值会被永久钉住，
            // 之后任何来源的变更都读不到
            return preferences.currentSnapshot()?.let { it[this] }
                ?: runBlocking { preferences.snapshot() }[this]
        }
        set(value) {
            pendingValue.store(PendingValue(value))
            preferences.edit { it[this@PrefDelegate] = value }
        }

    inline operator fun getValue(thisRef: Any?, prop: KProperty<*>?): T = value
    inline operator fun setValue(thisRef: Any?, prop: KProperty<*>?, value: T) {
        this.value = value
    }
}
