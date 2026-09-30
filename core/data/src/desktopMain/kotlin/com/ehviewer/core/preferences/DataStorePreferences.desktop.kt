package com.ehviewer.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

internal actual fun getDateStore(name: String?): DataStore<Preferences> {
    val actualName = name ?: "ehviewer_preferences"
    return PreferenceDataStoreFactory.createWithPath {
        preferencesPath(actualName)
    }
}

// 与 Room.desktop.kt 的数据目录约定保持一致
internal fun preferencesPath(name: String) = "${System.getenv("APPDATA")?.replace('\\', '/') ?: (System.getProperty("user.home") + "/.ehviewer")}/EhViewer/files/$name.preferences_pb"
    .toPath()
