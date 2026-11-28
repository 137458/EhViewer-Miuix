package com.ehviewer.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import splitties.init.appCtx

internal actual fun getDateStore(name: String?): DataStore<Preferences> {
    val context = appCtx
    val actualName = name ?: "${context.packageName}_preferences"
    return PreferenceDataStoreFactory.create(
        // 偏好文件损坏时按空偏好重建：不抛异常，用户设置回到默认值而不是启动崩溃
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        migrations = listOf(SharedPreferencesMigration(context = context, sharedPreferencesName = actualName)),
    ) {
        context.preferencesDataStoreFile(actualName)
    }
}
