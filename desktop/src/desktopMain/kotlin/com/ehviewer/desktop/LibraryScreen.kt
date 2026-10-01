package com.ehviewer.desktop

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.scrollbar.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ehviewer.core.database.client.getCategoryDisplayName
import com.ehviewer.core.database.client.thumbUrl
import com.ehviewer.core.database.model.GalleryEntity
import com.ehviewer.core.database.model.LocalFavoriteInfo
import com.ehviewer.core.i18n.MR
import com.ehviewer.core.model.BaseGalleryInfo
import com.ehviewer.core.network.EhCookieStore
import com.ehviewer.core.ui.component.GalleryListCardRating
import com.ehviewer.core.ui.component.VerticalScrollbar
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
    Favorites,
    Online,
    ;

    companion object {
        fun fromName(raw: String?): LibraryTab = runCatching { valueOf(raw!!) }.getOrDefault(History)
    }
}

// 本地库：历史列表(左) + 选中画廊详情(右) 主从双栏（桌面大屏习惯）+ 连接诊断行。
// 在线画廊列表需 HTML 解析下沉（Rust 专项），由后续轮次接入。
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onOpenGalleryInNewWindow: ((BaseGalleryInfo) -> Unit)? = null,
    openGalleryDialogVisible: Boolean = false,
    onOpenGalleryDialogOpen: () -> Unit = {},
    onOpenGalleryDialogClose: () -> Unit = {},
) {
    val downloadLabels by DesktopDatabase.eh.downloadsDao()
        .countByLabel()
        .collectAsState(initial = emptyMap())
    val favoriteCount by DesktopDatabase.eh.localFavoritesDao()
        .count()
        .collectAsState(initial = 0)
    val viewModeOrdinal by DesktopSettings.viewMode.valueFlow().collectAsState(DesktopSettings.viewMode.value)
    val viewMode = DesktopViewMode.fromOrdinal(viewModeOrdinal)
    var currentTab by remember { mutableStateOf(LibraryTab.fromName(DesktopSettings.lastTab.value)) }

    fun switchTab(tab: LibraryTab) {
        currentTab = tab
        DesktopSettings.lastTab.value = tab.name
    }
    // 历史/收藏走 Room Flow 响应式收集：开窗/收藏/删除后跨窗口自动刷新
    val history by DesktopDatabase.eh.historyDao()
        .listGalleriesFlow()
        .collectAsState(initial = emptyList())
    val historyTimes by DesktopDatabase.eh.historyDao()
        .listTimesFlow()
        .collectAsState(initial = emptyList())
    val historyTimeByGid = remember(historyTimes) { historyTimes.associate { it.gid to it.time } }
    val favoriteTimes by DesktopDatabase.eh.localFavoritesDao()
        .listTimesFlow()
        .collectAsState(initial = emptyList())
    val favoriteTimeByGid = remember(favoriteTimes) { favoriteTimes.associate { it.gid to it.time } }
    val favorites by DesktopDatabase.eh.localFavoritesDao()
        .listGalleriesFlow()
        .collectAsState(initial = emptyList())
    val favoriteGids = remember(favorites) { favorites.map { it.gid }.toSet() }
    var online by remember { mutableStateOf<List<BaseGalleryInfo>>(emptyList()) }
    var selected by remember { mutableStateOf<BaseGalleryInfo?>(null) }
    var connectionStatus by remember { mutableStateOf<DesktopConnectionStatus>(DesktopConnectionStatus.Checking) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var sortConfig by remember {
        mutableStateOf(DesktopSortConfig.decode(DesktopSettings.sortConfig.value))
    }
    val searchHistoryRaw by DesktopSettings.searchHistory.valueFlow()
        .collectAsState(DesktopSettings.searchHistory.value)
    val searchHistoryList = remember(searchHistoryRaw) {
        DesktopSearchHistory.decode(searchHistoryRaw)
    }
    var previewCoverUrl by remember { mutableStateOf<String?>(null) }
    var notifications by remember { mutableStateOf<List<DesktopNotification>>(emptyList()) }
    val nextNotificationId = remember { AtomicLong(1L) }
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val historyClearedMessage = stringResource(MR.strings.search_history_cleared)
    val checkingConnectionText = stringResource(MR.strings.desktop_status_checking)
    val connectingServerText = stringResource(MR.strings.desktop_online_connecting)
    val onlineEmptyText = stringResource(MR.strings.desktop_online_empty)
    val openingGalleryText = stringResource(MR.strings.desktop_notification_opening_gallery)
    val addedToFavoritesText = stringResource(MR.strings.add_to_favorite_success)
    val removedFromFavoritesText = stringResource(MR.strings.remove_from_favorite_success)
    val copiedText = stringResource(MR.strings.desktop_copied)
    val linkCopiedText = stringResource(MR.strings.desktop_link_copied)
    val removedFromHistoryText = stringResource(MR.strings.desktop_removed_from_history)
    val filterText = stringResource(MR.strings.desktop_filter)
    val noBrowserText = stringResource(MR.strings.no_browser_installed)

    fun showNotification(message: String) {
        val now = System.currentTimeMillis()
        notifications = DesktopNotificationManager.post(
            current = notifications,
            message = message,
            timestamp = now,
            idProvider = { nextNotificationId.getAndIncrement() },
        )
    }

    fun recordSearch(query: String) {
        val updated = DesktopSearchHistory.addQuery(searchHistoryList, query)
        DesktopSettings.searchHistory.value = DesktopSearchHistory.encode(updated)
    }

    fun clearSearchHistory() {
        DesktopSettings.searchHistory.value = ""
        showNotification(historyClearedMessage)
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
        connectionStatus = DesktopConnectionStatus.Checking
        runCatching {
            withContext(Dispatchers.IO) {
                desktopGet("https://e-hentai.org/home.php")
            }
        }.onSuccess { response ->
            val status = response.status
            connectionStatus = DesktopConnectionStatus.Online(status)
            logcat("Connection", LogPriority.INFO) { "EH_HOME status=$status" }
            if (response.status in 200..299) {
                runCatching {
                    // Rust 原生 HTML 解析移出主线程，避免大页面解析期间冻结 UI
                    withContext(Dispatchers.IO) {
                        val bytes = response.body.toByteArray()
                        val buffer = ByteBuffer.allocateDirect(bytes.size).put(bytes).apply { flip() }
                        parseGalleryList(buffer).galleryInfoList.toList()
                    }
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
            val cleaned = DesktopConnectionState.cleanErrorMessage(e.message ?: e::class.simpleName)
            connectionStatus = DesktopConnectionStatus.Offline(cleaned)
            logcat("Connection", LogPriority.WARN) { "EH_HOME failed: $cleaned (raw: ${e.message})" }
        }
    }

    fun toggleFavorite(gallery: BaseGalleryInfo) {
        val isFav = DesktopFavoritesState.isFavorite(favoriteGids, gallery.gid)
        coroutineScope.launch {
            if (isFav) {
                withContext(Dispatchers.IO) {
                    DesktopDatabase.eh.localFavoritesDao().deleteByKey(gallery.gid)
                }
                selected = DesktopFavoritesState.updateSelectionAfterRemoveFavorite(
                    selected = selected,
                    removedGid = gallery.gid,
                    isInFavoritesTab = currentTab == LibraryTab.Favorites,
                )
                showNotification(removedFromFavoritesText)
            } else {
                withContext(Dispatchers.IO) {
                    val entity = DesktopFavoritesState.toGalleryEntity(gallery)
                    DesktopDatabase.eh.galleryDao().upsert(entity)
                    DesktopDatabase.eh.localFavoritesDao().upsert(LocalFavoriteInfo(gallery.gid))
                }
                showNotification(addedToFavoritesText)
            }
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

    val currentItems: List<BaseGalleryInfo> = when (currentTab) {
        LibraryTab.History -> history
        LibraryTab.Favorites -> favorites
        LibraryTab.Online -> online
    }
    val filteredItems = remember(currentItems, searchQuery, sortConfig) {
        val filtered = GalleryFilter.filterGalleries(currentItems, searchQuery)
        sortConfig.sort(filtered)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (openGalleryDialogVisible) {
                        // 对话框打开期间只拦截 Escape 关闭，其余按键让给输入框
                        if (event.type == KeyEventType.KeyDown && !event.isCtrlPressed && event.key == Key.Escape) {
                            onOpenGalleryDialogClose()
                            true
                        } else {
                            false
                        }
                    } else {
                        val action = resolveKeyAction(
                            isKeyDown = event.type == KeyEventType.KeyDown,
                            isCtrlPressed = event.isCtrlPressed,
                            key = event.key,
                            hasSelection = selected != null,
                        )
                        when (action) {
                            DesktopKeyAction.ClearSelection -> {
                                if (previewCoverUrl != null) {
                                    previewCoverUrl = null
                                } else if (searchQuery.isNotEmpty()) {
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
                            DesktopKeyAction.SelectNext -> {
                                val next = DesktopNavigation.nextSelection(filteredItems, selected)
                                if (next != null) {
                                    selected = next
                                    true
                                } else {
                                    false
                                }
                            }
                            DesktopKeyAction.SelectPrevious -> {
                                val prev = DesktopNavigation.previousSelection(filteredItems, selected)
                                if (prev != null) {
                                    selected = prev
                                    true
                                } else {
                                    false
                                }
                            }
                            DesktopKeyAction.SelectFirst -> {
                                DesktopNavigation.nextSelection(filteredItems, null)?.let { selected = it }
                                filteredItems.isNotEmpty()
                            }
                            DesktopKeyAction.SelectLast -> {
                                DesktopNavigation.previousSelection(filteredItems, null)?.let { selected = it }
                                filteredItems.isNotEmpty()
                            }
                            DesktopKeyAction.OpenSelected -> {
                                if (searchQuery.isNotBlank()) {
                                    recordSearch(searchQuery)
                                }
                                selected?.let { gallery ->
                                    onOpenGalleryInNewWindow?.invoke(gallery)
                                }
                                true
                            }
                            DesktopKeyAction.OpenLinkDialog -> {
                                onOpenGalleryDialogOpen()
                                true
                            }
                            else -> false
                        }
                    }
                },
        ) {
            val info = updateInfo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val statusText = "e-hentai: " + DesktopConnectionState.formatStatus(
                        status = connectionStatus,
                        checkingLabel = stringResource(MR.strings.desktop_status_checking),
                        offlineLabel = stringResource(MR.strings.desktop_status_offline),
                    )
                    val isOffline = DesktopConnectionState.isOffline(connectionStatus)
                    val statusBgColor = when (connectionStatus) {
                        is DesktopConnectionStatus.Online -> MiuixTheme.colorScheme.surfaceVariant
                        is DesktopConnectionStatus.Offline -> MiuixTheme.colorScheme.error.copy(alpha = 0.12f)
                        DesktopConnectionStatus.Checking -> MiuixTheme.colorScheme.surfaceVariant
                    }
                    val statusTextColor = when (connectionStatus) {
                        is DesktopConnectionStatus.Online -> MiuixTheme.colorScheme.onBackground
                        is DesktopConnectionStatus.Offline -> MiuixTheme.colorScheme.error
                        DesktopConnectionStatus.Checking -> MiuixTheme.colorScheme.onSurfaceVariantSummary
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusBgColor)
                            .then(
                                if (DesktopConnectionState.canRetry(connectionStatus)) {
                                    Modifier
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .clickable {
                                            coroutineScope.launch {
                                                showNotification(checkingConnectionText)
                                                refreshGalleries()
                                            }
                                        }
                                } else {
                                    Modifier
                                },
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = statusText,
                                fontSize = 12.sp,
                                color = statusTextColor,
                            )
                            if (isOffline) {
                                Text(
                                    text = "↻ ${stringResource(MR.strings.action_retry)}",
                                    fontSize = 11.sp,
                                    color = MiuixTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }

                    Text(
                        text = stringResource(
                            MR.strings.desktop_library_status_bar,
                            EhCookieStore.hasSignedIn().toString(),
                            downloadLabels.size,
                            favoriteCount,
                            DESKTOP_VERSION,
                        ),
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }

                if (info != null) {
                    Text(
                        text = stringResource(MR.strings.desktop_update_available, info.tag),
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.primary,
                        modifier = Modifier
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable { openBrowser(info.pageUrl) },
                    )
                }
            }
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.width(320.dp).fillMaxHeight()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "${stringResource(MR.strings.history)} (${history.size})",
                                color = if (currentTab == LibraryTab.History) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable { switchTab(LibraryTab.History) }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                            )
                            Text(
                                text = "${stringResource(MR.strings.local_favorites)} (${favorites.size})",
                                color = if (currentTab == LibraryTab.Favorites) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable { switchTab(LibraryTab.Favorites) }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                            )
                            Text(
                                text = "${stringResource(MR.strings.online)} (${online.size})",
                                color = if (currentTab == LibraryTab.Online) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable { switchTab(LibraryTab.Online) }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                            )
                        }
                        if (currentTab == LibraryTab.History && history.isNotEmpty()) {
                            Text(
                                text = stringResource(MR.strings.clear_all),
                                color = MiuixTheme.colorScheme.primary,
                                modifier = Modifier
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable {
                                        coroutineScope.launch {
                                            withContext(Dispatchers.IO) {
                                                DesktopDatabase.eh.historyDao().deleteAll()
                                            }
                                            selected = DesktopHistoryState.updateSelectionAfterClearAll(
                                                currentSelected = selected,
                                                currentTabIsHistory = true,
                                            )
                                            showNotification(historyClearedMessage)
                                        }
                                    }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(MR.strings.search_hint)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (searchQuery.isNotBlank()) {
                                        recordSearch(searchQuery)
                                    }
                                },
                                onDone = {
                                    if (searchQuery.isNotBlank()) {
                                        recordSearch(searchQuery)
                                    }
                                },
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                                        if (searchQuery.isNotBlank()) {
                                            recordSearch(searchQuery)
                                        }
                                        false
                                    } else {
                                        false
                                    }
                                },
                        )
                        Text(
                            text = if (sortConfig.field == DesktopSortField.Default) {
                                stringResource(MR.strings.desktop_sort)
                            } else {
                                sortConfig.label
                            },
                            color = if (sortConfig.field == DesktopSortField.Default) MiuixTheme.colorScheme.onSurfaceVariantSummary else MiuixTheme.colorScheme.primary,
                            modifier = Modifier
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable {
                                    sortConfig = sortConfig.cycle().also {
                                        DesktopSettings.sortConfig.value = it.encode()
                                    }
                                }
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                        )
                        Text(
                            text = if (viewMode == DesktopViewMode.List) {
                                stringResource(MR.strings.desktop_view_list)
                            } else {
                                stringResource(MR.strings.desktop_view_grid)
                            },
                            color = MiuixTheme.colorScheme.primary,
                            modifier = Modifier
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable {
                                    DesktopSettings.viewMode.value = viewMode.toggle().ordinal
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                        )
                    }
                    val suggestions = remember(searchHistoryList, searchQuery) {
                        DesktopSearchHistory.filterSuggestions(searchHistoryList, searchQuery, maxSuggestions = 5)
                    }
                    if (suggestions.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "${stringResource(MR.strings.history)}:",
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                fontSize = 11.sp,
                            )
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val deleteLabel = stringResource(MR.strings.delete)
                                suggestions.forEach { suggestion ->
                                    ContextMenuArea(
                                        items = {
                                            listOf(
                                                ContextMenuItem(deleteLabel) {
                                                    val updated = DesktopSearchHistory.removeQuery(searchHistoryList, suggestion)
                                                    DesktopSettings.searchHistory.value = DesktopSearchHistory.encode(updated)
                                                },
                                            )
                                        },
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MiuixTheme.colorScheme.surfaceVariant)
                                                .pointerHoverIcon(PointerIcon.Hand)
                                                .clickable {
                                                    searchQuery = suggestion
                                                    recordSearch(suggestion)
                                                }
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                        ) {
                                            Text(
                                                text = suggestion,
                                                fontSize = 11.sp,
                                                color = MiuixTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable { clearSearchHistory() }
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = "✕",
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                    if (filteredItems.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                            if (searchQuery.isNotBlank() && currentItems.isNotEmpty()) {
                                Text(
                                    text = stringResource(MR.strings.desktop_empty_no_match),
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            } else {
                                when (currentTab) {
                                    LibraryTab.History -> Text(
                                        text = stringResource(MR.strings.desktop_empty_no_history),
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    )
                                    LibraryTab.Favorites -> Text(
                                        text = stringResource(MR.strings.desktop_empty_no_favorites),
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    )
                                    LibraryTab.Online -> {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            when (val status = connectionStatus) {
                                                is DesktopConnectionStatus.Offline -> {
                                                    Text(
                                                        text = stringResource(MR.strings.desktop_status_offline) + ": ${status.reason}",
                                                        color = MiuixTheme.colorScheme.error,
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(MiuixTheme.colorScheme.surfaceVariant)
                                                            .pointerHoverIcon(PointerIcon.Hand)
                                                            .clickable {
                                                                coroutineScope.launch {
                                                                    showNotification(checkingConnectionText)
                                                                    refreshGalleries()
                                                                }
                                                            }
                                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                                    ) {
                                                        Text(
                                                            text = stringResource(MR.strings.action_retry),
                                                            color = MiuixTheme.colorScheme.primary,
                                                            fontSize = 12.sp,
                                                        )
                                                    }
                                                }
                                                DesktopConnectionStatus.Checking -> {
                                                    Text(
                                                        text = connectingServerText,
                                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                                    )
                                                }
                                                is DesktopConnectionStatus.Online -> {
                                                    Text(
                                                        text = onlineEmptyText,
                                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val copyTitleLabel = stringResource(MR.strings.copy_title)
                        val copyLinkLabel = stringResource(MR.strings.copy_link)
                        val openBrowserLabel = stringResource(MR.strings.open_in_browser)
                        val openInNewWindowLabel = stringResource(MR.strings.menu_new_window)
                        val deleteLabel = stringResource(MR.strings.delete)
                        val addFavoriteLabel = stringResource(MR.strings.add_favorites_dialog_title)
                        val deleteFavoriteLabel = stringResource(MR.strings.delete_favorites_dialog_title)

                        fun buildGalleryContextMenu(
                            gallery: BaseGalleryInfo,
                            title: String,
                            link: String,
                        ): List<ContextMenuItem> {
                            val menuItems = mutableListOf(
                                ContextMenuItem(copyTitleLabel) {
                                    clipboard.setText(AnnotatedString(title))
                                    showNotification("$copiedText: $title")
                                },
                                ContextMenuItem(copyLinkLabel) {
                                    clipboard.setText(AnnotatedString(link))
                                    showNotification(linkCopiedText)
                                },
                                ContextMenuItem(openBrowserLabel) {
                                    openBrowser(link)
                                },
                                ContextMenuItem(
                                    if (DesktopFavoritesState.isFavorite(favoriteGids, gallery.gid)) {
                                        deleteFavoriteLabel
                                    } else {
                                        addFavoriteLabel
                                    },
                                ) {
                                    toggleFavorite(gallery)
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
                                            selected = DesktopHistoryState.updateSelectionAfterDelete(selected, gallery.gid)
                                            showNotification(removedFromHistoryText)
                                        }
                                    },
                                )
                            }
                            return menuItems
                        }

                        if (viewMode == DesktopViewMode.List) {
                            val listState = rememberLazyListState()
                            Row(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                                    items(
                                        filteredItems.size,
                                        key = { index -> filteredItems[index].gid },
                                    ) { index ->
                                        val gallery = filteredItems[index]
                                        val title = galleryDisplayTitle(gallery.title, gallery.gid)
                                        val link = galleryWebUrl(gallery.gid, gallery.token)
                                        ContextMenuArea(
                                            items = { buildGalleryContextMenu(gallery, title, link) },
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
                                                val browseTime = when (currentTab) {
                                                    LibraryTab.History -> historyTimeByGid[gallery.gid]
                                                    LibraryTab.Favorites -> favoriteTimeByGid[gallery.gid]
                                                    LibraryTab.Online -> null
                                                }
                                                browseTime?.let { time ->
                                                    Text(
                                                        text = java.time.Instant.ofEpochMilli(time)
                                                            .atZone(java.time.ZoneId.systemDefault())
                                                            .format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm")),
                                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                                        fontSize = 11.sp,
                                                    )
                                                }
                                                Text(
                                                    text = getCategoryDisplayName(gallery.category),
                                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                                )
                                            }
                                        }
                                    }
                                }
                                VerticalScrollbar(
                                    adapter = rememberScrollbarAdapter(listState),
                                    isScrollInProgress = listState.isScrollInProgress,
                                )
                            }
                        } else {
                            val gridState = rememberLazyGridState()
                            Row(modifier = Modifier.fillMaxSize()) {
                                LazyVerticalGrid(
                                    state = gridState,
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(
                                        filteredItems.size,
                                        key = { index -> filteredItems[index].gid },
                                    ) { index ->
                                        val gallery = filteredItems[index]
                                        val title = galleryDisplayTitle(gallery.title, gallery.gid)
                                        val link = galleryWebUrl(gallery.gid, gallery.token)
                                        ContextMenuArea(
                                            items = { buildGalleryContextMenu(gallery, title, link) },
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
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
                                                            MiuixTheme.colorScheme.surfaceVariant
                                                        },
                                                    )
                                                    .padding(8.dp),
                                            ) {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                                ) {
                                                    gallery.thumbUrl?.let { thumb ->
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(110.dp)
                                                                .clip(RoundedCornerShape(6.dp)),
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            AsyncImage(
                                                                model = thumb,
                                                                contentDescription = title,
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop,
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = title,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        color = MiuixTheme.colorScheme.onBackground,
                                                    )
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        GalleryListCardRating(rating = gallery.rating)
                                                        Text(
                                                            text = DesktopRating.formatCardMeta(gallery.pages, gallery.category),
                                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                VerticalScrollbar(
                                    adapter = rememberScrollbarAdapter(gridState),
                                    isScrollInProgress = gridState.isScrollInProgress,
                                )
                            }
                        }
                    }
                }
                VerticalDivider()
                selected?.let { gallery ->
                    GalleryDetailPane(
                        gallery = gallery,
                        isFavorite = DesktopFavoritesState.isFavorite(favoriteGids, gallery.gid),
                        onToggleFavorite = { toggleFavorite(gallery) },
                        onCopy = { value, label ->
                            clipboard.setText(AnnotatedString(value))
                            showNotification(if (label.isBlank()) copiedText else "$copiedText: $label")
                            logcat("Library", LogPriority.INFO) { "Copied $label" }
                        },
                        onOpenUrl = { url ->
                            if (!DesktopBrowser.openUrl(url)) {
                                showNotification(noBrowserText)
                            }
                        },
                        onSearchTag = { tag ->
                            searchQuery = tag
                            recordSearch(tag)
                            showNotification("$filterText: $tag")
                        },
                        onPreviewCover = { url -> previewCoverUrl = url },
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

        previewCoverUrl?.let { coverUrl ->
            CoverPreviewDialog(
                imageUrl = coverUrl,
                onDismiss = { previewCoverUrl = null },
            )
        }

        if (openGalleryDialogVisible) {
            OpenGalleryDialog(
                onDismiss = onOpenGalleryDialogClose,
                onOpen = { target ->
                    onOpenGalleryDialogClose()
                    showNotification("$openingGalleryText ${target.gid}")
                    onOpenGalleryInNewWindow?.invoke(DesktopOpenGalleryState.createGalleryInfo(target))
                },
            )
        }
    }
}

@Composable
private fun OpenGalleryDialog(
    onDismiss: () -> Unit,
    onOpen: (GalleryParsedTarget) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var attempted by remember { mutableStateOf(false) }
    val target = remember(input) { DesktopOpenGalleryState.parseInput(input) }
    val errorText = if (attempted) {
        when (DesktopOpenGalleryState.validate(input)) {
            DesktopOpenGalleryError.EmptyInput -> stringResource(MR.strings.desktop_open_gallery_empty_input)
            DesktopOpenGalleryError.InvalidInput -> stringResource(MR.strings.desktop_open_gallery_invalid_input)
            null -> null
        }
    } else {
        null
    }
    val title = stringResource(MR.strings.desktop_open_gallery_title)
    val hint = stringResource(MR.strings.desktop_open_gallery_hint)
    val cancelLabel = stringResource(MR.strings.desktop_action_cancel)
    val openLabel = stringResource(MR.strings.desktop_open_gallery_action_open)

    DesktopModalCard(
        title = title,
        onDismiss = onDismiss,
        cardWidth = 440.dp,
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = {
                input = it
                attempted = false
            },
            placeholder = { Text(hint) },
            singleLine = true,
            isError = errorText != null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (target != null) onOpen(target) else attempted = true
                },
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        if (errorText != null) {
            Text(
                text = errorText,
                color = MiuixTheme.colorScheme.error,
                fontSize = 12.sp,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = cancelLabel,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
            Text(
                text = openLabel,
                color = if (target != null) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable {
                        if (target != null) onOpen(target) else attempted = true
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
internal fun GalleryDetailPane(
    gallery: BaseGalleryInfo,
    onCopy: (value: String, label: String) -> Unit,
    onOpenUrl: ((url: String) -> Unit)? = null,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onSearchTag: ((tag: String) -> Unit)? = null,
    onPreviewCover: ((url: String) -> Unit)? = null,
) {
    val detailScrollState = rememberScrollState()
    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(detailScrollState)
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
                val imageState = remember(thumb) { DesktopImageStateController() }
                val decodeErrorText = stringResource(MR.strings.decode_image_error)
                val retryActionText = stringResource(MR.strings.action_retry)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable { onPreviewCover?.invoke(thumb) },
                    contentAlignment = Alignment.Center,
                ) {
                    key(thumb, imageState.retryCount) {
                        AsyncImage(
                            model = thumb,
                            contentDescription = displayTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            onLoading = { imageState.onLoading() },
                            onSuccess = { imageState.onSuccess() },
                            onError = { err -> imageState.onError(err.result.throwable.message) },
                        )
                    }
                    if (imageState.canRetry) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MiuixTheme.colorScheme.surfaceVariant)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable { imageState.retry() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "$decodeErrorText ($retryActionText)",
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
            HorizontalDivider()
            DetailRow(label = stringResource(MR.strings.key_gid), value = gallery.gid.toString(), onCopy = onCopy)
            DetailRow(label = stringResource(MR.strings.key_token), value = gallery.token, onCopy = onCopy)
            DetailRow(label = stringResource(MR.strings.key_uploader), value = gallery.uploader.orEmpty().ifEmpty { "-" }, onCopy = onCopy)
            DetailRow(label = stringResource(MR.strings.key_category), value = getCategoryDisplayName(gallery.category), onCopy = onCopy)
            DetailRow(label = stringResource(MR.strings.key_pages), value = gallery.pages.toString(), onCopy = onCopy)
            DetailRow(
                label = stringResource(MR.strings.key_rating),
                value = DesktopRating.formatRatingScore(gallery.rating),
                onCopy = onCopy,
                extraContent = { GalleryListCardRating(rating = gallery.rating) },
            )
            gallery.simpleLanguage?.let { DetailRow(label = stringResource(MR.strings.key_language), value = it, onCopy = onCopy) }
            if (onToggleFavorite != null) {
                DetailRow(
                    label = stringResource(MR.strings.favorite_name),
                    value = if (isFavorite) stringResource(MR.strings.key_favorited) else stringResource(MR.strings.not_favorited),
                    onCopy = onCopy,
                    actionText = if (isFavorite) stringResource(MR.strings.delete_favorites_dialog_title) else stringResource(MR.strings.add_favorites_dialog_title),
                    onAction = onToggleFavorite,
                )
            }
            DetailRow(
                label = stringResource(MR.strings.key_url),
                value = link,
                onCopy = onCopy,
                onOpen = onOpenUrl?.let { opener -> { opener(link) } },
            )
            gallery.thumbUrl?.let { DetailRow(label = stringResource(MR.strings.key_thumb), value = it, onCopy = onCopy) }
            DetailRow(
                label = stringResource(MR.strings.action_share),
                value = stringResource(MR.strings.action_copy),
                onCopy = onCopy,
                actionText = stringResource(MR.strings.action_copy),
                onAction = {
                    val summary = DesktopTagFormatter.generateShareSummary(
                        title = displayTitle,
                        gid = gallery.gid,
                        token = gallery.token,
                        rating = gallery.rating,
                        pages = gallery.pages,
                        category = gallery.category,
                        tags = gallery.simpleTags?.toList(),
                    )
                    onCopy(summary, "")
                },
            )
            gallery.simpleTags?.takeIf { it.isNotEmpty() }?.let { tags ->
                val grouped = remember(tags) { DesktopTagFormatter.groupTags(tags.toList()) }
                if (grouped.isNotEmpty()) {
                    val tagLabel = stringResource(MR.strings.search_sft)
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "$tagLabel (${DesktopTagFormatter.splitTags(tags.toList()).size})",
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            fontSize = 12.sp,
                        )
                        grouped.forEach { (namespace, tagList) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    text = "$namespace:",
                                    color = MiuixTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.width(60.dp).padding(top = 2.dp),
                                )
                                FlowRow(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    tagList.forEach { tagName ->
                                        val fullTag = DesktopTagFormatter.formatTagQuery(namespace, tagName)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MiuixTheme.colorScheme.surfaceVariant)
                                                .pointerHoverIcon(PointerIcon.Hand)
                                                .clickable {
                                                    if (onSearchTag != null) {
                                                        onSearchTag(fullTag)
                                                    } else {
                                                        onCopy(fullTag, tagLabel)
                                                    }
                                                }
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                        ) {
                                            Text(
                                                text = tagName,
                                                fontSize = 11.sp,
                                                color = MiuixTheme.colorScheme.onBackground,
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(detailScrollState),
            isScrollInProgress = detailScrollState.isScrollInProgress,
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    onCopy: (value: String, label: String) -> Unit,
    onOpen: (() -> Unit)? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    extraContent: (@Composable () -> Unit)? = null,
) {
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
        if (extraContent != null) {
            extraContent()
        }
        if (actionText != null && onAction != null) {
            Text(
                text = actionText,
                color = MiuixTheme.colorScheme.primary,
                modifier = Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable { onAction() },
            )
        }
        if (onOpen != null) {
            Text(
                text = stringResource(MR.strings.open_in_browser),
                color = MiuixTheme.colorScheme.primary,
                modifier = Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable { onOpen() },
            )
        }
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
    DesktopBrowser.openUrl(url)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CoverPreviewDialog(
    imageUrl: String,
    onDismiss: () -> Unit,
) {
    var scale by remember { mutableStateOf(1.0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.85f)
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = stringResource(MR.strings.key_thumb),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .pointerHoverIcon(PointerIcon.Hand)
                    .combinedClickable(
                        onClick = {},
                        onDoubleClick = { scale = DesktopZoomController.toggleFitZoom(scale) },
                    ),
                contentScale = ContentScale.Fit,
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "−",
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable { scale = DesktopZoomController.zoomOut(scale) }
                    .padding(horizontal = 8.dp),
            )
            Text(
                text = DesktopZoomController.formatZoomPercentage(scale),
                color = MiuixTheme.colorScheme.primary,
                style = MiuixTheme.textStyles.body2,
                modifier = Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable { scale = DesktopZoomController.resetZoom() },
            )
            Text(
                text = "+",
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable { scale = DesktopZoomController.zoomIn(scale) }
                    .padding(horizontal = 8.dp),
            )
            VerticalDivider()
            Text(
                text = "✕",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 4.dp),
            )
        }
    }
}
