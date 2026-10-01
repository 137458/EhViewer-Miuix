package com.ehviewer.desktop

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ehviewer.core.database.model.GalleryEntity
import com.ehviewer.core.network.EhCookieStore
import com.ehviewer.core.util.LogPriority
import com.ehviewer.core.util.logcat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 本地库：历史列表(左) + 选中画廊详情(右) 主从双栏（桌面大屏习惯）+ 连接诊断行。
// 在线画廊列表需 HTML 解析下沉（Rust 专项），由后续轮次接入。
@Composable
fun LibraryScreen() {
    val downloadLabels by DesktopDatabase.eh.downloadsDao()
        .countByLabel()
        .collectAsState(initial = emptyMap())
    val favoriteCount by DesktopDatabase.eh.localFavoritesDao()
        .count()
        .collectAsState(initial = 0)
    var history by remember { mutableStateOf<List<GalleryEntity>>(emptyList()) }
    var selected by remember { mutableStateOf<GalleryEntity?>(null) }
    var httpStatusCode by remember { mutableStateOf<Int?>(null) }
    var connectionError by remember { mutableStateOf<String?>(null) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        history = withContext(Dispatchers.IO) {
            DesktopDatabase.eh.historyDao().listGalleries()
        }
        runCatching {
            withContext(Dispatchers.IO) {
                desktopGet("https://e-hentai.org/home.php")
            }
        }.onSuccess { response ->
            connectionError = null
            httpStatusCode = response.status
            logcat("Connection", LogPriority.INFO) { "EH_HOME status=${response.status}" }
        }.onFailure { e ->
            connectionError = e.message ?: e::class.simpleName
            logcat("Connection", LogPriority.WARN) { "EH_HOME failed: $connectionError" }
        }
        checkLatestRelease()?.let { info ->
            if (isNewer(info.tag, DESKTOP_VERSION)) {
                updateInfo = info
                logcat("Update", LogPriority.INFO) { "UPDATE_AVAILABLE tag=${info.tag}" }
            } else {
                logcat("Update", LogPriority.INFO) { "UP_TO_DATE current=$DESKTOP_VERSION latest=${info.tag}" }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val info = updateInfo
        Text(
            text = buildString {
                append(
                    "e-hentai: " + when {
                        httpStatusCode != null -> "HTTP $httpStatusCode"
                        connectionError != null -> "offline"
                        else -> "checking..."
                    },
                )
                append("  |  signed-in: ${EhCookieStore.hasSignedIn()}")
                append("  |  download groups: ${downloadLabels.size}  |  favorites: $favoriteCount")
                append("  |  version: $DESKTOP_VERSION")
                if (info != null) append("  |  update available: ${info.tag}")
            },
            color = if (info != null) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(8.dp)
                .let { m -> if (info != null) m.clickable { openBrowser(info.pageUrl) } else m },
        )
        HorizontalDivider()
        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.width(320.dp).fillMaxHeight()) {
                Text(
                    "History (${history.size})",
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp),
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(history.size) { index ->
                        val gallery = history[index]
                        val title = gallery.title.orEmpty().ifEmpty { gallery.gid.toString() }
                        ContextMenuArea(
                            items = {
                                listOf(
                                    ContextMenuItem("Copy title") {
                                        clipboard.setText(AnnotatedString(title))
                                    },
                                )
                            },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { selected = gallery }
                                    .background(
                                        if (selected?.gid == gallery.gid) {
                                            MiuixTheme.colorScheme.secondaryContainer
                                        } else {
                                            Color.Unspecified
                                        },
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
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
            VerticalDivider()
            selected?.let { gallery ->
                GalleryDetailPane(
                    gallery = gallery,
                    onCopy = { value, label ->
                        clipboard.setText(AnnotatedString(value))
                        logcat("Library", LogPriority.INFO) { "Copied $label" }
                    },
                )
            }
        }
    }
}

@Composable
private fun GalleryDetailPane(gallery: GalleryEntity, onCopy: (value: String, label: String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = gallery.title.orEmpty().ifEmpty { "Untitled" },
            color = MiuixTheme.colorScheme.primary,
        )
        gallery.titleJpn?.takeIf { it.isNotEmpty() }?.let {
            Text(text = it, color = MiuixTheme.colorScheme.onBackground)
        }
        HorizontalDivider()
        DetailRow(label = "GID", value = gallery.gid.toString(), onCopy = onCopy)
        DetailRow(label = "Token", value = gallery.token, onCopy = onCopy)
        DetailRow(label = "Uploader", value = gallery.uploader.orEmpty().ifEmpty { "-" }, onCopy = onCopy)
        DetailRow(label = "Category", value = gallery.category.toString(), onCopy = onCopy)
        DetailRow(label = "Pages", value = gallery.pages.toString(), onCopy = onCopy)
        DetailRow(label = "Rating", value = gallery.rating.toString(), onCopy = onCopy)
        gallery.simpleLanguage?.let { DetailRow(label = "Language", value = it, onCopy = onCopy) }
        gallery.simpleTags?.takeIf { it.isNotEmpty() }?.let { tags ->
            Text(
                text = tags.joinToString(", "),
                color = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, onCopy: (value: String, label: String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = MiuixTheme.colorScheme.onBackground,
            modifier = Modifier.width(80.dp),
        )
        Text(
            text = value,
            color = MiuixTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "Copy",
            color = MiuixTheme.colorScheme.primary,
            modifier = Modifier.clickable { onCopy(value, label) },
        )
    }
}

@Composable
private fun VerticalDivider() = HorizontalDivider(
    modifier = Modifier.fillMaxHeight().width(1.dp),
    color = MiuixTheme.colorScheme.outline,
    thickness = 1.dp,
)

private fun openBrowser(url: String) {
    runCatching {
        java.awt.Desktop.getDesktop().browse(java.net.URI(url))
    }.onFailure { e ->
        logcat("Update", LogPriority.WARN) { "open browser failed: ${e.message}" }
    }
}
