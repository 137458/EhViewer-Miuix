package com.hippo.ehviewer.ui.update

import android.content.Context
import android.widget.Toast
import com.ehviewer.core.i18n.R
import com.hippo.ehviewer.util.installPackage
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 所有安装入口的统一出口：异常一律转成可见提示，不再静默吞掉或崩溃。
 * installPackage 内部抛出的 IllegalStateException（未授予安装权限）也会落到这里。
 * 安装前先校验更新包身份（包名 + 签名证书），不合格的包直接拒绝并提示原因。
 */
internal suspend fun installUpdatePackage(context: Context, file: File) {
    withContext(Dispatchers.Main) {
        val rejection = UpdateApkVerifier.verify(context, file)
        if (rejection != null) {
            Toast.makeText(
                context,
                context.getString(R.string.update_failed, rejection),
                Toast.LENGTH_LONG,
            ).show()
            return@withContext
        }
        runCatching { with(context) { installPackage(file) } }.onFailure { error ->
            Toast.makeText(
                context,
                context.getString(
                    R.string.update_failed,
                    error.localizedMessage ?: error.javaClass.simpleName,
                ),
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}
