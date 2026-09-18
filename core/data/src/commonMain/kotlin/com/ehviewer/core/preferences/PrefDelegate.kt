package com.ehviewer.core.preferences

import androidx.datastore.preferences.core.Preferences
import kotlin.concurrent.Volatile
import kotlin.reflect.KProperty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

@Suppress("NOTHING_TO_INLINE")
class PrefDelegate<T> internal constructor(
    private val preferences: DataStorePreferences,
    val key: Preferences.Key<T & Any>,
    val defaultValue: T,
) {
    @Volatile
    private var localCachedValue: T? = null

    @Volatile
    private var hasLocalCache = false

    fun changesFlow(): Flow<Unit> = valueFlow().drop(1).map {}

    fun valueFlow(): Flow<T> = preferences.data.map { it[this] }.distinctUntilChanged()

    var value: T
        @Suppress("UNCHECKED_CAST")
        get() {
            preferences.currentSnapshot()?.let { return it[this] }
            if (hasLocalCache) return localCachedValue as T
            val v = runBlocking { preferences.snapshot() }[this]
            localCachedValue = v
            hasLocalCache = true
            return v
        }
        set(value) {
            localCachedValue = value
            hasLocalCache = true
            preferences.edit { it[this@PrefDelegate] = value }
        }

    inline operator fun getValue(thisRef: Any?, prop: KProperty<*>?): T = value
    inline operator fun setValue(thisRef: Any?, prop: KProperty<*>?, value: T) {
        this.value = value
    }
}
