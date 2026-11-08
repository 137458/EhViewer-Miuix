package com.hippo.ehviewer.ui.update

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ehviewer.core.i18n.R
import com.hippo.ehviewer.ui.MainActivity

/**
 * 进程内的前台标记。下载完成时据此决定「直接拉起安装」还是「发通知」，
 * 避免后台状态下 startActivity 被系统拦截后用户毫无反馈。
 */
internal object AppForegroundTracker {
    @Volatile
    var isForeground: Boolean = false
}

/**
 * 更新下载的后台通知。完成通知带「安装」动作，点击由 MainActivity 承接安装，
 * 不在后台直接 startActivity。
 */
internal object UpdateNotifications {
    const val ACTION_INSTALL_UPDATE = "com.hippo.ehviewer.action.INSTALL_UPDATE"

    private const val CHANNEL_ID = "app_update"
    private const val ID_DONE = 1001
    private const val ID_FAILED = 1002

    fun notifyDownloaded(context: Context) {
        post(
            context = context,
            id = ID_DONE,
            title = context.getString(R.string.update_notification_done_title),
            text = context.getString(R.string.update_notification_done_text),
            installAction = true,
            icon = android.R.drawable.stat_sys_download_done,
        )
    }

    fun notifyFailed(context: Context, message: String) {
        post(
            context = context,
            id = ID_FAILED,
            title = context.getString(R.string.update_notification_failed_title),
            text = message,
            installAction = false,
            icon = com.hippo.ehviewer.R.drawable.ic_baseline_warning_24,
        )
    }

    private fun post(
        context: Context,
        id: Int,
        title: String,
        text: String,
        installAction: Boolean,
        icon: Int,
    ) {
        if (!canNotify(context)) return
        ensureChannel(context)

        val openApp = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (installAction) action = ACTION_INSTALL_UPDATE
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            id,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(if (installAction) NotificationCompat.CATEGORY_RECOMMENDATION else NotificationCompat.CATEGORY_ERROR)
        if (installAction) {
            builder.addAction(
                icon,
                context.getString(R.string.update_dialog_btn_install),
                contentIntent,
            )
        }

        runCatching { NotificationManagerCompat.from(context).notify(id, builder.build()) }
    }

    private fun ensureChannel(context: Context) {
        runCatching {
            NotificationManagerCompat.from(context).createNotificationChannel(
                NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName(context.getString(R.string.update_notification_channel))
                    .build(),
            )
        }
    }

    private fun canNotify(context: Context): Boolean {
        val enabled = runCatching { NotificationManagerCompat.from(context).areNotificationsEnabled() }
            .getOrDefault(false)
        if (!enabled) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
}
