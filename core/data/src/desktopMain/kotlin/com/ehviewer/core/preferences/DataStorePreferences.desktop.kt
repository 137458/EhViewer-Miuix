package com.ehviewer.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.ehviewer.core.DesktopDirs

internal actual fun getDateStore(name: String?): DataStore<Preferences> {
    val actualName = name ?: "ehviewer_preferences"
    return PreferenceDataStoreFactory.createWithPath(
        // 偏好文件损坏时按空偏好重建：不抛异常，用户设置回到默认值而不是启动崩溃
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    ) {
        preferencesPath(actualName)
    }
}

// 偏好落盘位置与 Room/Cookie 同源（DesktopDirs.filesDir）
internal fun preferencesPath(name: String) = DesktopDirs.filesDir / "$name.preferences_pb"
