package com.hippo.ehviewer.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.serializer
import splitties.init.appCtx

val dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
val dataStore = PreferenceDataStoreFactory.create { appCtx.preferencesDataStoreFile("Remembered") }
val dataStateFlow = dataStore.data.stateIn(dataStoreScope, SharingStarted.Eagerly, emptyPreferences())

// 存储内容可能因版本升级或序列化契约变更而无法解码，此时回落到默认值而不是让界面崩溃
@PublishedApi
internal fun <T> decodeStoredValue(serializer: KSerializer<T>, bytes: ByteArray): T? = runCatching { Cbor.decodeFromByteArray(serializer, bytes) }.getOrNull()

// Find out how make this work with any generic `Serializable` type
@Composable
inline fun <reified T> rememberMutableStateInDataStore(
    key: String,
    serializer: KSerializer<T> = serializer<T>(),
    crossinline defaultValue: @DisallowComposableCalls () -> T,
) = with(dataStateFlow) {
    remember(key) {
        val keyObj = byteArrayPreferencesKey(key)
        val r = value[keyObj]?.let { bytes -> decodeStoredValue(serializer, bytes) } ?: defaultValue()
        mutableStateOf(r)
    }.also { mutableState ->
        LifecycleResumeEffect(key) {
            onPauseOrDispose {
                dataStoreScope.launch {
                    val keyObj = byteArrayPreferencesKey(key)
                    dataStore.edit { p ->
                        p[keyObj] = Cbor.encodeToByteArray(serializer, mutableState.value)
                    }
                }
            }
        }
    }
}
