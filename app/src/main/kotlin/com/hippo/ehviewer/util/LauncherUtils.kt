package com.hippo.ehviewer.util

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.VisualMediaType
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.ehviewer.core.i18n.R
import com.ehviewer.core.util.isAtLeastO
import com.ehviewer.core.util.withIOContext
import com.ehviewer.core.util.withUIContext
import java.io.File
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.fetchAndIncrement
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

// Fuck off the silly Android launcher and callback :)

private val atomicInteger = AtomicInt(0)

context(ctx: Context)
private val lifecycle: Lifecycle
    get() {
        var context: Context? = ctx
        while (true) {
            when (context) {
                is LifecycleOwner -> return context.lifecycle
                !is ContextWrapper -> error("This should never happen!")
                else -> context = context.baseContext
            }
        }
    }

// 已注销过再注销会抛 IllegalStateException，清理路径只关心别打断回调链
private fun ActivityResultLauncher<*>?.unregisterQuietly() {
    runCatching { this?.unregister() }
}

context(_: Context)
suspend fun <I, O> awaitActivityResult(contract: ActivityResultContract<I, O>, input: I): O {
    val key = "activity_rq#${atomicInteger.fetchAndIncrement()}"
    var launcher: ActivityResultLauncher<I>? = null
    var continuation: CancellableContinuation<O>? = null
    lateinit var observer: LifecycleEventObserver
    observer = LifecycleEventObserver { _, event ->
        if (Lifecycle.Event.ON_DESTROY == event) {
            launcher.unregisterQuietly()
            lifecycle.removeObserver(observer)
            // Activity 销毁后结果永远不会回来，取消挂起的协程而不是让它一直等
            continuation?.cancel()
        }
    }
    return withUIContext {
        lifecycle.addObserver(observer)
        suspendCancellableCoroutine { cont ->
            continuation = cont
            val activity = findActivity<ComponentActivity>()
            launcher = activity.activityResultRegistry.register(key, contract) {
                launcher.unregisterQuietly()
                lifecycle.removeObserver(observer)
                if (cont.isActive) cont.resume(it)
            }.apply {
                try {
                    launch(input)
                } catch (t: Throwable) {
                    // 没有 Activity 能处理该 Intent 时会抛 ActivityNotFoundException，
                    // 这时必须撤掉刚注册的 launcher 与观察者，否则回调链会一直挂着
                    unregisterQuietly()
                    lifecycle.removeObserver(observer)
                    throw t
                }
            }
            cont.invokeOnCancellation {
                // 调用方取消（超时/页面退出）时同样要注销，否则 registry 里会残留条目
                launcher.unregisterQuietly()
                lifecycle.removeObserver(observer)
            }
        }
    }
}

context(ctx: Context)
suspend fun requestPermission(key: String): Boolean {
    if (ContextCompat.checkSelfPermission(ctx, key) == PackageManager.PERMISSION_GRANTED) return true
    return awaitActivityResult(ActivityResultContracts.RequestPermission(), key)
}

context(_: Context)
suspend fun pickVisualMedia(type: VisualMediaType): Uri? = awaitActivityResult(ActivityResultContracts.PickVisualMedia(), PickVisualMediaRequest(mediaType = type))

@RequiresApi(Build.VERSION_CODES.O)
context(ctx: Context)
suspend fun requestInstallPermission(): Boolean = with(ctx) {
    if (packageManager.canRequestPackageInstalls()) return true
    val activity = runCatching { findActivity<ComponentActivity>() }.getOrNull()
    if (activity != null) {
        awaitActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                "package:$packageName".toUri(),
            ),
        )
    } else {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            "package:$packageName".toUri(),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }
    return packageManager.canRequestPackageInstalls()
}

context(ctx: Context)
suspend fun installPackage(file: File) = with(ctx) {
    val canInstall = !isAtLeastO || packageManager.canRequestPackageInstalls() || requestInstallPermission()
    // 不能静默返回：调用方拿不到任何信号，用户点「安装」会毫无反应
    check(canInstall) { getString(R.string.permission_denied) }
    val contentUri = withIOContext { FileProvider.getUriForFile(ctx, "$packageName.fileprovider", file) }
    val intent = Intent(Intent.ACTION_VIEW).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        setDataAndType(contentUri, "application/vnd.android.package-archive")
    }
    startActivity(intent)
}
