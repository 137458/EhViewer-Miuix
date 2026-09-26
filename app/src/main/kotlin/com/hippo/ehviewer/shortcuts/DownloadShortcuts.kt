package com.hippo.ehviewer.shortcuts

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.ehviewer.core.i18n.R as I18nR
import com.ehviewer.core.util.isAtLeastO
import com.hippo.ehviewer.R
import com.hippo.ehviewer.download.DownloadService

private const val ID_START_ALL = "start_all"
private const val ID_STOP_ALL = "stop_all"

// 动态快捷方式由系统以发布方身份启动，targetPackage 取自运行时包名，
// 不会像静态 shortcuts.xml 那样被 applicationIdSuffix 打断
private fun Context.downloadShortcut(
    id: String,
    action: String,
    @StringRes labelRes: Int,
    @DrawableRes iconRes: Int,
) = ShortcutInfoCompat.Builder(this, id)
    .setShortLabel(getString(labelRes))
    .setLongLabel(getString(labelRes))
    .setIcon(IconCompat.createWithResource(this, iconRes))
    .setIntent(Intent(this, ShortcutsActivity::class.java).setAction(action))
    .build()

// 供 ShortcutsActivity 上报使用情况，桌面据此调整快捷方式排序
internal val shortcutIds = mapOf(
    DownloadService.ACTION_START_ALL to ID_START_ALL,
    DownloadService.ACTION_STOP_ALL to ID_STOP_ALL,
)

fun Context.updateDownloadShortcuts() {
    // 快捷方式图标只有 v26 及以上的资源变体
    if (!isAtLeastO) return
    ShortcutManagerCompat.setDynamicShortcuts(
        this,
        listOf(
            downloadShortcut(
                ID_START_ALL,
                DownloadService.ACTION_START_ALL,
                I18nR.string.download_start_all,
                R.drawable.ic_shortcut_start,
            ),
            downloadShortcut(
                ID_STOP_ALL,
                DownloadService.ACTION_STOP_ALL,
                I18nR.string.download_stop_all,
                R.drawable.ic_shortcut_stop,
            ),
        ),
    )
}
