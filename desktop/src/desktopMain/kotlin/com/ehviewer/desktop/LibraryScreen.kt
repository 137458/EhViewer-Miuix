package com.ehviewer.desktop

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ehviewer.core.database.client.thumbUrl
import com.ehviewer.core.database.model.GalleryEntity
import com.ehviewer.core.i18n.MR
import com.ehviewer.core.model.BaseGalleryInfo
import com.ehviewer.core.network.EhCookieStore
import com.ehviewer.core.util.LogPriority
import com.ehviewer.core.util.logcat
import com.hippo.ehviewer.client.parser.GalleryListParserKtProbe
import com.hippo.ehviewer.client.parser.parseGalleryList
import dev.icerock.moko.resources.compose.stringResource
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.theme.MiuixTheme

fun galleryWebUrl(gid: Long, token: String): String = "https://e-hentai.org/g/$gid/$token/"

fun galleryDisplayTitle(title: String?, gid: Long): String = title?.trim()?.takeIf { it.isNotEmpty() } ?: gid.toString()

fun formatGalleryTags(tags: List<String>?): String = tags?.joinToString(", ") ?: ""

enum class LibraryTab {
    History,
    Online,
}

// 本地库：历史列表(左) + 选中画廊详情(右) 主从双栏（桌面大屏习惯）+ 连接诊断行。
// 在线画廊列表需 HTML 解析下沉（Rust 专项），由后续轮次接入。
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onOpenGalleryInNewWindow: ((BaseGalleryInfo) -> Unit)? = null,
) {
    val downloadLabels by DesktopDatabase.eh.downloadsDao()
        .countByLabel()
        .collectAsState(initial = emptyMap())
    val favoriteCount by DesktopDatabase.eh.localFavoritesDao()
        .count()
        .collectAsState(initial = 0)
    var currentTab by remember { mutableStateOf(LibraryTab.History) }
    var history by remember { mutableStateOf<List<GalleryEntity>>(emptyList()) }
    var online by remember { mutableStateOf<List<BaseGalleryInfo>>(emptyList()) }
    var selected by remember { mutableStateOf<BaseGalleryInfo?>(null) }
    var httpStatusCode by remember { mutableStateOf<Int?>(null) }
    var connectionError by remember { mutableStateOf<String?>(null) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var notifications by remember { mutableStateOf<List<DesktopNotification>>(emptyList()) }
    val nextNotificationId = remember { AtomicLong(1L) }
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    fun showNotification(message: String) {
        val now = System.currentTimeMillis()
        notifications = DesktopNotificationManager.post(
            current = notifications,
            message = message,
            timestamp = now,
            idProvider = { nextNotificationId.getAndIncrement() },
        )
    }

    LaunchedEffect(notifications) {
        if (notifications.isNotEmpty()) {
            delay(2500L)
            notifications = DesktopNotificationManager.expire(
                current = notifications,
                currentTime = System.currentTimeMillis(),
                ttlMs = 2500L,
            )
        }
    }

    suspend fun refreshGalleries() {
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
            if (response.status in 200..299) {
                runCatching {
                    val bytes = response.body.toByteArray()
                    val buffer = ByteBuffer.allocateDirect(bytes.size).put(bytes).apply { flip() }
                    parseGalleryList(buffer).galleryInfoList.toList()
                }.onSuccess { list ->
                    online = list
                    logcat("Library", LogPriority.INFO) { "ONLINE_LIST parsed=${list.size}" }
                }.onFailure { e ->
                    logcat("Library", LogPriority.WARN) {
                        "ONLINE_LIST parse failed: $e | loadErr=${GalleryListParserKtProbe.loadError} | " +
                            "res=${GalleryListParserKtProbe.resAvailable} | cwd=${java.io.File(".").absolutePath}"
                    }
                }
            }
        }.onFailure { e ->
            connectionError = e.message ?: e::class.simpleName
            logcat("Connection", LogPriority.WARN) { "EH_HOME failed: $connectionError" }
        }
    }

    LaunchedEffect(Unit) {
        refreshGalleries()
        checkLatestRelease()?.let { info ->
            if (isNewer(info.tag, DESKTOP_VERSION)) {
                updateInfo = info
                logcat("Update", LogPriority.INFO) { "UPDATE_AVAILABLE tag=${info.tag}" }
            } else {
                logcat("Update", LogPriority.INFO) { "UP_TO_DATE current=$DESKTOP_VERSION latest=${info.tag}" }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    val action = resolveKeyAction(
                        isKeyDown = event.type == KeyEventType.KeyDown,
                        isCtrlPressed = event.isCtrlPressed,
                        key = event.key,
                        hasSelection = selected != null,
                    )
                    when (action) {
                        DesktopKeyAction.ClearSelection -> {
                            if (searchQuery.isNotEmpty()) {
                                searchQuery = ""
                            } else {
                                selected = null
                            }
                            true
                        }
                        DesktopKeyAction.Refresh -> {
                            coroutineScope.launch { refreshGalleries() }
                            true
                        }
                        else -> false
                    }
                },
        ) {
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
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "${stringResource(MR.strings.history)} (${history.size})",
                            color = if (currentTab == LibraryTab.History) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier
                                .clickable { currentTab = LibraryTab.History }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                        )
                        Text(
                            text = "${stringResource(MR.strings.online)} (${online.size})",
                            color = if (currentTab == LibraryTab.Online) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier
                                .clickable { currentTab = LibraryTab.Online }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                        )
                    }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(MR.strings.search_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                    HorizontalDivider()
                    val currentItems: List<BaseGalleryInfo> = if (currentTab == LibraryTab.History) history else online
                    val filteredItems = remember(currentItems, searchQuery) {
                        GalleryFilter.filterGalleries(currentItems, searchQuery)
                    }
                    if (filteredItems.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (searchQuery.isNotBlank() && currentItems.isNotEmpty()) {
                                    "No matching galleries"
                                } else if (currentTab == LibraryTab.History) {
                                    "No history recorded"
                                } else if (connectionError != null) {
                                    "Offline: $connectionError"
                                } else {
                                    "Loading online galleries..."
                                },
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    } else {
                        val copyTitleLabel = stringResource(MR.strings.copy_title)
                        val copyLinkLabel = stringResource(MR.strings.copy_link)
                        val openBrowserLabel = stringResource(MR.strings.open_in_browser)
                        val openInNewWindowLabel = stringResource(MR.strings.menu_new_window)
                        val deleteLabel = stringResource(MR.strings.delete)
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(filteredItems.size) { index ->
                                val gallery = filteredItems[index]
                                val title = galleryDisplayTitle(gallery.title, gallery.gid)
                                val link = galleryWebUrl(gallery.gid, gallery.token)
                                ContextMenuArea(
                                    items = {
                                        val menuItems = mutableListOf(
                                            ContextMenuItem(copyTitleLabel) {
                                                clipboard.setText(AnnotatedString(title))
                                                showNotification("Copied: $title")
                                            },
                                            ContextMenuItem(copyLinkLabel) {
                                                clipboard.setText(AnnotatedString(link))
                                                showNotification("Copied link")
                                            },
                                            ContextMenuItem(openBrowserLabel) {
                                                openBrowser(link)
                                            },
                                        )
                                        if (onOpenGalleryInNewWindow != null) {
                                            menuItems.add(
                                                ContextMenuItem(openInNewWindowLabel) {
                                                    onOpenGalleryInNewWindow(gallery)
                                                },
                                            )
                                        }
                                        if (currentTab == LibraryTab.History) {
                                            menuItems.add(
                                                ContextMenuItem(deleteLabel) {
                                                    coroutineScope.launch {
                                                        withContext(Dispatchers.IO) {
                                                            DesktopDatabase.eh.historyDao().deleteByKey(gallery.gid)
                                                        }
                                                        history = DesktopHistoryState.removeGallery(history, gallery.gid)
                                                        selected = DesktopHistoryState.updateSelectionAfterDelete(selected, gallery.gid)
                                                        showNotification("Removed from history")
                                                    }
                                                },
                                            )
                                        }
                                        menuItems
                                    },
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .pointerHoverIcon(PointerIcon.Hand)
                                            .combinedClickable(
                                                onClick = { selected = gallery },
                                                onDoubleClick = {
                                                    selected = gallery
                                                    onOpenGalleryInNewWindow?.invoke(gallery)
                                                },
                                            )
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
                }
                VerticalDivider()
                selected?.let { gallery ->
                    GalleryDetailPane(
                        gallery = gallery,
                        onCopy = { value, label ->
                            clipboard.setText(AnnotatedString(value))
                            showNotification("Copied $label")
                            logcat("Library", LogPriority.INFO) { "Copied $label" }
                        },
                    )
                }
            }
        }

        if (notifications.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End,
            ) {
                notifications.forEach { notice ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable {
                                notifications = DesktopNotificationManager.dismiss(notifications, notice.id)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = notice.message,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun GalleryDetailPane(gallery: BaseGalleryInfo, onCopy: (value: String, label: String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val displayTitle = galleryDisplayTitle(gallery.title, gallery.gid)
        val link = galleryWebUrl(gallery.gid, gallery.token)
        Text(
            text = displayTitle,
            color = MiuixTheme.colorScheme.primary,
        )
        gallery.titleJpn?.takeIf { it.isNotEmpty() }?.let {
            Text(text = it, color = MiuixTheme.colorScheme.onBackground)
        }
        gallery.thumbUrl?.let { thumb ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = thumb,
                    contentDescription = displayTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        HorizontalDivider()
        DetailRow(label = stringResource(MR.strings.key_gid), value = gallery.gid.toString(), onCopy = onCopy)
        DetailRow(label = stringResource(MR.strings.key_token), value = gallery.token, onCopy = onCopy)
        DetailRow(label = stringResource(MR.strings.key_uploader), value = gallery.uploader.orEmpty().ifEmpty { "-" }, onCopy = onCopy)
        DetailRow(label = stringResource(MR.strings.key_category), value = gallery.category.toString(), onCopy = onCopy)
        DetailRow(label = stringResource(MR.strings.key_pages), value = gallery.pages.toString(), onCopy = onCopy)
        DetailRow(label = stringResource(MR.strings.key_rating), value = gallery.rating.toString(), onCopy = onCopy)
        gallery.simpleLanguage?.let { DetailRow(label = stringResource(MR.strings.key_language), value = it, onCopy = onCopy) }
        DetailRow(label = stringResource(MR.strings.key_url), value = link, onCopy = onCopy)
        gallery.thumbUrl?.let { DetailRow(label = stringResource(MR.strings.key_thumb), value = it, onCopy = onCopy) }
        gallery.simpleTags?.takeIf { it.isNotEmpty() }?.let { tags ->
            Text(
                text = formatGalleryTags(tags),
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
            text = stringResource(MR.strings.action_copy),
            color = MiuixTheme.colorScheme.primary,
            modifier = Modifier
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable { onCopy(value, label) },
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
