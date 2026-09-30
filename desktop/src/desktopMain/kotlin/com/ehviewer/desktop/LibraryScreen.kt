package com.ehviewer.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ehviewer.core.database.model.GalleryEntity
import com.ehviewer.core.network.EhCookieStore
import com.ehviewer.core.util.LogPriority
import com.ehviewer.core.util.logcat
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 本地库概览 + 阅读历史 + 连接诊断：桌面首次消费共享 Room Flow 与网络栈(EhCookieStore 注入)。
// 在线画廊列表需 HTML 解析下沉，由后续轮次接入。
@Composable
fun LibraryScreen() {
    val downloadLabels by DesktopDatabase.eh.downloadsDao()
        .countByLabel()
        .collectAsState(initial = emptyMap())
    val favoriteCount by DesktopDatabase.eh.localFavoritesDao()
        .count()
        .collectAsState(initial = 0)
    var history by remember { mutableStateOf<List<GalleryEntity>>(emptyList()) }
    var httpStatus by remember { mutableStateOf<HttpStatusCode?>(null) }
    var connectionError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        history = withContext(Dispatchers.IO) {
            DesktopDatabase.eh.historyDao().listGalleries()
        }
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val status = httpStatus
        Text(
            "e-hentai: " + when {
                status != null -> "HTTP ${status.value}"
                connectionError != null -> "offline"
                else -> "checking..."
            } + "  |  signed-in: ${EhCookieStore.hasSignedIn()}" +
                "  |  download groups: ${downloadLabels.size}  |  favorites: $favoriteCount",
            color = MiuixTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Text(
            "History (${history.size})",
            color = MiuixTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(history.size) { index ->
                val gallery = history[index]
                val title = gallery.title.orEmpty().ifEmpty { gallery.gid.toString() }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = MiuixTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = gallery.category.toString(),
                        color = MiuixTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}
