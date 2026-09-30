package com.ehviewer.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.ehviewer.core.DesktopDirs

internal actual fun getDateStore(name: String?): DataStore<Preferences> {
    val actualName = name ?: "ehviewer_preferences"
    return PreferenceDataStoreFactory.createWithPath {
        preferencesPath(actualName)
    }
}

// 偏好落盘位置与 Room/Cookie 同源（DesktopDirs.filesDir）
internal fun preferencesPath(name: String) = DesktopDirs.filesDir / "$name.preferences_pb"
