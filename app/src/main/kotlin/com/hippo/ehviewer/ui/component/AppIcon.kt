package com.hippo.ehviewer.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap

/**
 * 当前应用图标。
 *
 * 取不到（如包管理器异常）时返回 null，由调用方回退到内置图标资源。
 */
@Composable
fun rememberAppIconBitmap(): ImageBitmap? {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(context.packageName, 0)
            pm.getApplicationIcon(appInfo).toBitmap().asImageBitmap()
        }.getOrNull()
    }
}
