package com.ehviewer.desktop

import com.ehviewer.core.database.EhDatabase
import com.ehviewer.core.database.roomDb

// 桌面侧主库：与 Android 同一 schema，落在 %APPDATA%/EhViewer/databases/eh.db
object DesktopDatabase {
    val eh: EhDatabase by lazy { roomDb<EhDatabase>("eh.db") }
}
