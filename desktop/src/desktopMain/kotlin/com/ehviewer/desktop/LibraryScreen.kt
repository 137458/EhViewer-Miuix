package com.ehviewer.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ehviewer.core.network.EhCookieStore
import com.ehviewer.core.util.LogPriority
import com.ehviewer.core.util.logcat
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 本地库概览 + 连接诊断：桌面首次消费共享 Room Flow 与网络栈(EhCookieStore 注入)。
// 画廊列表/详情需 HTML 解析下沉，由后续轮次接入。
@Composable
fun LibraryScreen() {
    val downloadLabels by DesktopDatabase.eh.downloadsDao()
        .countByLabel()
        .collectAsState(initial = emptyMap())
    val favoriteCount by DesktopDatabase.eh.localFavoritesDao()
        .count()
        .collectAsState(initial = 0)
    var httpStatus by remember { mutableStateOf<HttpStatusCode?>(null) }
    var connectionError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching {
            withContext(Dispatchers.IO) {
                acquireClient().get("https://e-hentai.org/home.php")
            }
        }.onSuccess { response: HttpResponse ->
            httpStatus = response.status
            logcat("Connection", LogPriority.INFO) { "EH_HOME status=${response.status}" }
        }.onFailure { e ->
            connectionError = e.message ?: e::class.simpleName
            logcat("Connection", LogPriority.WARN) { "EH_HOME failed: $connectionError" }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Download groups: ${downloadLabels.size}", color = MiuixTheme.colorScheme.primary)
        Text("Local favorites: $favoriteCount", color = MiuixTheme.colorScheme.onBackground)
        val status = httpStatus
        Text(
            "e-hentai: " + when {
                status != null -> "HTTP ${status.value}"
                connectionError != null -> "offline"
                else -> "checking..."
            },
            color = MiuixTheme.colorScheme.onBackground,
        )
        Text(
            "signed-in: ${EhCookieStore.hasSignedIn()}",
            color = MiuixTheme.colorScheme.onBackground,
        )
    }
}
