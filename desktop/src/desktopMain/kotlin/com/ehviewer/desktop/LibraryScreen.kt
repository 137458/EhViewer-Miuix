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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.ehviewer.core.database.client.thumbUrl
import com.ehviewer.core.database.model.GalleryEntity
import com.ehviewer.core.database.model.HistoryInfo
import com.ehviewer.core.database.model.LocalFavoriteInfo
import com.ehviewer.core.i18n.MR
import com.ehviewer.core.model.BaseGalleryInfo
import com.ehviewer.core.network.EhCookieStore
import com.ehviewer.core.ui.component.GalleryListCardRating
import com.ehviewer.core.ui.component.SquircleShape
import com.ehviewer.core.ui.component.VerticalScrollbar
import com.ehviewer.core.util.LogPriority
import com.ehviewer.core.util.logcat
import com.hippo.ehviewer.client.parser.GalleryDetailParser
import com.hippo.ehviewer.client.parser.GalleryListParserKtProbe
import com.hippo.ehviewer.client.parser.parseGalleryList
import dev.icerock.moko.resources.compose.stringResource
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
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
    onOpenGallery: ((BaseGalleryInfo) -> Unit)? = null,
    onOpenReader: ((BaseGalleryInfo) -> Unit)? = null,
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
    var remoteSearchQuery by remember { mutableStateOf("") }

    // 游标导航栈：记录每次远程搜索请求的 next 游标（null=第一页），支撑双向翻页
    val cursorStack = remember { mutableStateListOf<Long?>(null) }
    var cursorIndex by remember { mutableIntStateOf(0) }
    // 批量多选：Ctrl+点击 toggle，行删除同步移除，切 Tab/刷新清空
    val multiSelection = remember { DesktopMultiSelection() }
    // Ctrl 按住状态跟踪（根 onPreviewKeyEvent 维护，供行 Ctrl+点击多选判定）
    var ctrlDown by remember { mutableStateOf(false) }

    fun switchTab(tab: LibraryTab) {
        currentTab = tab
        multiSelection.clear()
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
    val searchFieldState = rememberTextFieldState()
    val searchQuery = searchFieldState.text.toString()
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
    // 一键清空历史前的确认对话框（毁灭性操作防误触）
    var showClearHistoryConfirm by remember { mutableStateOf(false) }
    // 批量删除选中项前的确认对话框（比单条删除波及更广，与清空历史同级防护）
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
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
    val undoText = stringResource(MR.strings.desktop_notification_undo)
    val filterText = stringResource(MR.strings.desktop_filter)
    val noBrowserText = stringResource(MR.strings.no_browser_installed)
    // 连接错误标签包：cleanErrorMessage 无组合语境，经注入走 i18n
    val connectionErrorLabels = DesktopConnectionErrorLabels(
        networkUnavailable = stringResource(MR.strings.desktop_error_network_unavailable),
        dnsUnresolved = stringResource(MR.strings.desktop_error_dns_unresolved),
        connectionRefused = stringResource(MR.strings.desktop_error_connection_refused),
        connectionTimedOut = stringResource(MR.strings.desktop_error_connection_timed_out),
        sslHandshakeFailed = stringResource(MR.strings.desktop_error_ssl_handshake_failed),
    )

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

    fun remoteSearch(query: String, nextGid: Long? = null, pushCursor: Boolean = true) {
        val url = DesktopSearchUrl.build(query, nextGid = nextGid) ?: return
        remoteSearchQuery = query
        if (pushCursor) {
            // 新方向搜索：截断回退分支后压入本次游标
            while (cursorStack.size > cursorIndex + 1) cursorStack.removeAt(cursorStack.size - 1)
            cursorStack.add(nextGid)
            cursorIndex += 1
        }
        // 列表即将被新页替换，清除跨页残留的选中态
        selected = null
        coroutineScope.launch {
            online = emptyList()
            connectionStatus = DesktopConnectionStatus.Checking
            runCatching {
                withContext(Dispatchers.IO) { desktopGet(url) }
            }.onSuccess { response ->
                // 与 refreshGalleries 同约定：拿到响应即定状态，避免非 2xx/解析失败时卡在 Checking 且无重试入口
                connectionStatus = DesktopConnectionStatus.Online(response.status)
                if (response.status in 200..299) {
                    runCatching {
                        parseGalleryList(response.toByteBuffer() ?: error("HTTP ${response.status}")).galleryInfoList.toList()
                    }.onSuccess { list ->
                        online = list
                        logcat("Library", LogPriority.INFO) { "ONLINE_SEARCH parsed=${list.size} q=$query next=$nextGid" }
                    }.onFailure { e ->
                        logcat("Library", LogPriority.WARN) { "ONLINE_SEARCH parse failed: $e" }
                    }
                } else {
                    logcat("Library", LogPriority.WARN) { "ONLINE_SEARCH status=${response.status}" }
                }
            }.onFailure { e ->
                val cleaned = DesktopConnectionState.cleanErrorMessage(e.message ?: e::class.simpleName, connectionErrorLabels)
                connectionStatus = DesktopConnectionStatus.Offline(cleaned)
                logcat("Library", LogPriority.WARN) { "ONLINE_SEARCH failed: $cleaned" }
            }
        }
    }

    fun clearSearchHistory() {
        DesktopSettings.searchHistory.value = ""
        showNotification(historyClearedMessage)
    }

    AutoExpireNotifications(notifications) { notifications = it }

    suspend fun refreshGalleries() {
        connectionStatus = DesktopConnectionStatus.Checking
        runCatching {
            withContext(Dispatchers.IO) {
                // 首页即画廊列表（未登录可用）；home.php 需登录，未登录会被弹到 bounce_login 导致解析失败
                desktopGet("https://e-hentai.org/")
            }
        }.onSuccess { response ->
            val status = response.status
            connectionStatus = DesktopConnectionStatus.Online(status)
            logcat("Connection", LogPriority.INFO) { "EH_HOME status=$status" }
            if (response.status in 200..299) {
                runCatching {
                    // Rust 原生 HTML 解析移出主线程，避免大页面解析期间冻结 UI
                    withContext(Dispatchers.IO) {
                        parseGalleryList(response.toByteBuffer() ?: error("HTTP ${response.status}")).galleryInfoList.toList()
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
            val cleaned = DesktopConnectionState.cleanErrorMessage(e.message ?: e::class.simpleName, connectionErrorLabels)
            connectionStatus = DesktopConnectionStatus.Offline(cleaned)
            logcat("Connection", LogPriority.WARN) { "EH_HOME failed: $cleaned (raw: ${e.message})" }
        }
    }

    // 搜索提交：Online Tab 走远程搜索（结果替换在线列表），空查询且处于远程搜索态则恢复默认列表，其余 Tab 维持本地过滤
    fun submitSearch(query: String) {
        val q = query.trim()
        if (q.isEmpty()) {
            if (currentTab == LibraryTab.Online && remoteSearchQuery.isNotBlank()) {
                remoteSearchQuery = ""
                coroutineScope.launch { refreshGalleries() }
            }
            return
        }
        recordSearch(q)
        if (currentTab == LibraryTab.Online) {
            remoteSearch(q)
        }
    }

    // 跨窗口搜索总线：详情窗口标签点击等场景发起的搜索在此消费（一次性，消费即清空）
    LaunchedEffect(DesktopSearchBus.pendingQuery) {
        val requested = DesktopSearchBus.pendingQuery ?: return@LaunchedEffect
        searchFieldState.setTextAndPlaceCursorAtEnd(requested)
        submitSearch(requested)
        DesktopSearchBus.consume()
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
                // 撤销 = 重新写回收藏行（GALLERIES 行未被删除，联表视图自动恢复）
                notifications = DesktopNotificationManager.post(
                    current = notifications,
                    message = removedFromFavoritesText,
                    timestamp = System.currentTimeMillis(),
                    idProvider = { nextNotificationId.getAndIncrement() },
                    actionLabel = undoText,
                    onAction = {
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                DesktopDatabase.eh.localFavoritesDao().upsert(LocalFavoriteInfo(gallery.gid))
                            }
                        }
                    },
                )
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
        // 更新检查延后 3s：避开与在线列表首拉争抢启动期网络
        delay(3_000L)
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
                    // 跟踪 Ctrl 按住状态（KeyDown/KeyUp 双沿），供行 Ctrl+点击批量多选判定
                    if (event.key == Key.CtrlLeft || event.key == Key.CtrlRight) {
                        ctrlDown = event.type == KeyEventType.KeyDown
                    }
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
                                    searchFieldState.clearText()
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
                                    submitSearch(searchQuery)
                                }
                                selected?.let { gallery ->
                                    onOpenGallery?.invoke(gallery)
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
                    val statusText = stringResource(
                        MR.strings.desktop_status_ehentai,
                        DesktopConnectionState.formatStatus(
                            status = connectionStatus,
                            checkingLabel = stringResource(MR.strings.desktop_status_checking),
                            offlineLabel = stringResource(MR.strings.desktop_status_offline),
                        ),
                    )
                    val isOffline = DesktopConnectionState.isOffline(connectionStatus)
                    val statusBgColor = when (connectionStatus) {
                        is DesktopConnectionStatus.Online -> MiuixTheme.colorScheme.surfaceContainerHighest
                        is DesktopConnectionStatus.Offline -> MiuixTheme.colorScheme.error.copy(alpha = 0.12f)
                        DesktopConnectionStatus.Checking -> MiuixTheme.colorScheme.surfaceContainerHighest
                    }
                    val statusTextColor = when (connectionStatus) {
                        is DesktopConnectionStatus.Online -> MiuixTheme.colorScheme.onBackground
                        is DesktopConnectionStatus.Offline -> MiuixTheme.colorScheme.error
                        DesktopConnectionStatus.Checking -> MiuixTheme.colorScheme.onSurfaceVariantSummary
                    }
                    Box(
                        modifier = Modifier
                            .clip(SquircleShape(4.dp))
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
                            .clickable { DesktopBrowser.openUrl(info.pageUrl) },
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
                                    .clickable { showClearHistoryConfirm = true }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextField(
                            state = searchFieldState,
                            label = stringResource(MR.strings.search_hint),
                            lineLimits = TextFieldLineLimits.SingleLine,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            onKeyboardAction = {
                                if (searchQuery.isNotBlank()) {
                                    submitSearch(searchQuery)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                                        if (searchQuery.isNotBlank()) {
                                            submitSearch(searchQuery)
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
                                                .clip(SquircleShape(4.dp))
                                                .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                                                .pointerHoverIcon(PointerIcon.Hand)
                                                .clickable {
                                                    searchFieldState.setTextAndPlaceCursorAtEnd(suggestion)
                                                    submitSearch(suggestion)
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
                                    .clip(SquircleShape(4.dp))
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
                    if (currentTab == LibraryTab.Online && remoteSearchQuery.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "\"$remoteSearchQuery\"",
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Text(
                                text = "✕",
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable {
                                        remoteSearchQuery = ""
                                        cursorStack.clear()
                                        cursorStack.add(null)
                                        cursorIndex = 0
                                        coroutineScope.launch { refreshGalleries() }
                                    }
                                    .padding(horizontal = 4.dp),
                            )
                            Text(
                                text = stringResource(MR.strings.desktop_reader_prev),
                                color = if (cursorIndex > 0) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable(enabled = cursorIndex > 0) {
                                        cursorIndex -= 1
                                        remoteSearch(remoteSearchQuery, nextGid = cursorStack[cursorIndex], pushCursor = false)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                            Text(
                                text = stringResource(MR.strings.desktop_online_page_n, cursorIndex + 1),
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                fontSize = 11.sp,
                            )
                            Box(modifier = Modifier.weight(1f))
                            Text(
                                text = stringResource(MR.strings.desktop_online_next),
                                color = if (online.isNotEmpty()) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable(enabled = online.isNotEmpty()) {
                                        val lastGid = online.lastOrNull()?.gid
                                        if (lastGid != null) {
                                            while (cursorStack.size > cursorIndex + 1) cursorStack.removeAt(cursorStack.size - 1)
                                            cursorStack.add(lastGid)
                                            cursorIndex += 1
                                            remoteSearch(remoteSearchQuery, nextGid = lastGid, pushCursor = false)
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
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
                                            if (remoteSearchQuery.isNotBlank() && connectionStatus is DesktopConnectionStatus.Online) {
                                                // 远程搜索已完成但无结果：与连接失败区分
                                                Text(
                                                    text = stringResource(MR.strings.desktop_search_no_results) + ": $remoteSearchQuery",
                                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                                )
                                            }
                                            when (val status = connectionStatus) {
                                                is DesktopConnectionStatus.Offline -> {
                                                    Text(
                                                        text = stringResource(MR.strings.desktop_status_offline) + ": ${status.reason}",
                                                        color = MiuixTheme.colorScheme.error,
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(SquircleShape(6.dp))
                                                            .background(MiuixTheme.colorScheme.surfaceContainerHighest)
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
                        val openGalleryLabel = stringResource(MR.strings.desktop_open_gallery_title)
                        val readLabel = stringResource(MR.strings.menu_read)
                        val deleteLabel = stringResource(MR.strings.delete)
                        val addFavoriteLabel = stringResource(MR.strings.add_favorites_dialog_title)
                        val deleteFavoriteLabel = stringResource(MR.strings.delete_favorites_dialog_title)
                        val batchDeleteLabel = stringResource(MR.strings.desktop_delete_selected, multiSelection.gids.size)

                        fun buildGalleryContextMenu(
                            gallery: BaseGalleryInfo,
                            title: String,
                            link: String,
                        ): List<ContextMenuItem> {
                            val menuItems = mutableListOf<ContextMenuItem>()
                            // 批量多选激活时置顶批量删除（按当前 Tab 作用于历史或收藏）；毁灭性操作先确认
                            if (multiSelection.isActive && (currentTab == LibraryTab.History || currentTab == LibraryTab.Favorites)) {
                                menuItems.add(
                                    ContextMenuItem(batchDeleteLabel) {
                                        showBatchDeleteConfirm = true
                                    },
                                )
                            }
                            if (onOpenReader != null) {
                                menuItems.add(
                                    ContextMenuItem(readLabel) {
                                        onOpenReader(gallery)
                                    },
                                )
                            }
                            if (onOpenGallery != null) {
                                menuItems.add(
                                    ContextMenuItem(openGalleryLabel) {
                                        onOpenGallery(gallery)
                                    },
                                )
                            }
                            menuItems.add(
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
                            menuItems.add(
                                ContextMenuItem(copyTitleLabel) {
                                    clipboard.setText(AnnotatedString(title))
                                    showNotification("$copiedText: $title")
                                },
                            )
                            menuItems.add(
                                ContextMenuItem(copyLinkLabel) {
                                    clipboard.setText(AnnotatedString(link))
                                    showNotification(linkCopiedText)
                                },
                            )
                            menuItems.add(
                                ContextMenuItem(openBrowserLabel) {
                                    DesktopBrowser.openUrl(link)
                                },
                            )
                            if (currentTab == LibraryTab.History) {
                                menuItems.add(
                                    ContextMenuItem(deleteLabel) {
                                        coroutineScope.launch {
                                            withContext(Dispatchers.IO) {
                                                DesktopDatabase.eh.historyDao().deleteByKey(gallery.gid)
                                            }
                                            selected = DesktopHistoryState.updateSelectionAfterDelete(selected, gallery.gid)
                                            // 撤销 = 重新写回该 gid 的历史行（时间戳刷新置顶）
                                            notifications = DesktopNotificationManager.post(
                                                current = notifications,
                                                message = removedFromHistoryText,
                                                timestamp = System.currentTimeMillis(),
                                                idProvider = { nextNotificationId.getAndIncrement() },
                                                actionLabel = undoText,
                                                onAction = {
                                                    coroutineScope.launch {
                                                        withContext(Dispatchers.IO) {
                                                            DesktopDatabase.eh.historyDao()
                                                                .upsert(HistoryInfo(gallery.gid))
                                                        }
                                                    }
                                                },
                                            )
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
                                                    .clip(SquircleShape(8.dp))
                                                    .pointerHoverIcon(PointerIcon.Hand)
                                                    .combinedClickable(
                                                        onClick = {
                                                            // Ctrl+点击进入/退出批量多选；普通点击收敛多选并单选（组合期捕获 WindowInfo，点击时读实时修饰键）
                                                            if (ctrlDown) {
                                                                multiSelection.toggle(gallery.gid)
                                                            } else {
                                                                multiSelection.clear()
                                                                selected = gallery
                                                            }
                                                        },
                                                        onDoubleClick = {
                                                            multiSelection.clear()
                                                            selected = gallery
                                                            onOpenGallery?.invoke(gallery)
                                                        },
                                                    )
                                                    .background(
                                                        when {
                                                            gallery.gid in multiSelection.gids -> MiuixTheme.colorScheme.primary.copy(alpha = 0.20f)
                                                            selected?.gid == gallery.gid -> MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                            else -> Color.Transparent
                                                        },
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                gallery.thumbUrl?.let { thumb ->
                                                    AsyncImage(
                                                        model = thumb,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .width(42.dp)
                                                            .height(56.dp)
                                                            .clip(SquircleShape(4.dp)),
                                                        contentScale = ContentScale.Crop,
                                                    )
                                                }
                                                Text(
                                                    text = title,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f),
                                                    color = MiuixTheme.colorScheme.onBackground,
                                                )
                                                val browseTime = when (currentTab) {
                                                    LibraryTab.History -> historyTimeByGid[gallery.gid]?.let { time ->
                                                        java.time.Instant.ofEpochMilli(time)
                                                            .atZone(java.time.ZoneId.systemDefault())
                                                            .format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"))
                                                    }
                                                    LibraryTab.Favorites -> favoriteTimeByGid[gallery.gid]?.let { time ->
                                                        java.time.Instant.ofEpochMilli(time)
                                                            .atZone(java.time.ZoneId.systemDefault())
                                                            .format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"))
                                                    }
                                                    // 在线列表：解析器已给出发布时间字符串（yyyy-MM-dd HH:mm）
                                                    LibraryTab.Online -> gallery.posted?.take(10)
                                                }
                                                browseTime?.let { time ->
                                                    Text(
                                                        text = time,
                                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                                        fontSize = 11.sp,
                                                    )
                                                }
                                                Text(
                                                    text = DesktopCategories.displayName(gallery.category),
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
                                                    .clip(SquircleShape(8.dp))
                                                    .pointerHoverIcon(PointerIcon.Hand)
                                                    .combinedClickable(
                                                        onClick = {
                                                            // Ctrl+点击进入/退出批量多选；普通点击收敛多选并单选（组合期捕获 WindowInfo，点击时读实时修饰键）
                                                            if (ctrlDown) {
                                                                multiSelection.toggle(gallery.gid)
                                                            } else {
                                                                multiSelection.clear()
                                                                selected = gallery
                                                            }
                                                        },
                                                        onDoubleClick = {
                                                            multiSelection.clear()
                                                            selected = gallery
                                                            onOpenGallery?.invoke(gallery)
                                                        },
                                                    )
                                                    .background(
                                                        when {
                                                            gallery.gid in multiSelection.gids -> MiuixTheme.colorScheme.primary.copy(alpha = 0.20f)
                                                            selected?.gid == gallery.gid -> MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                            else -> MiuixTheme.colorScheme.surfaceContainerHighest
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
                                                                .clip(SquircleShape(6.dp)),
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
                                                            text = DesktopRating.formatCardMeta(gallery.pages, DesktopCategories.displayName(gallery.category)),
                                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                        )
                                                    }
                                                    if (currentTab == LibraryTab.Online) {
                                                        gallery.posted?.take(10)?.let { postedDate ->
                                                            Text(
                                                                text = postedDate,
                                                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                                                fontSize = 11.sp,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                            )
                                                        }
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
                            searchFieldState.setTextAndPlaceCursorAtEnd(tag)
                            recordSearch(tag)
                            showNotification("$filterText: $tag")
                        },
                        onPreviewCover = { url -> previewCoverUrl = url },
                        onOpenReader = onOpenReader?.let { opener -> { opener(gallery) } },
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
                            .clip(SquircleShape(8.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable {
                                notifications = DesktopNotificationManager.dismiss(notifications, notice.id)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = notice.message,
                                color = MiuixTheme.colorScheme.onSurface,
                            )
                            // 带动作的通知（如历史删除撤销）：点击动作区执行并随通知一并消失
                            if (notice.actionLabel != null) {
                                Text(
                                    text = notice.actionLabel,
                                    color = MiuixTheme.colorScheme.primary,
                                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                                        .clickable {
                                            notice.onAction?.invoke()
                                            notifications = DesktopNotificationManager.dismiss(notifications, notice.id)
                                        },
                                )
                            }
                        }
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
                    onOpenGallery?.invoke(DesktopOpenGalleryState.createGalleryInfo(target))
                },
            )
        }

        if (showClearHistoryConfirm) {
            val confirmTitle = stringResource(MR.strings.clear_all_history)
            val cancelLabel = stringResource(MR.strings.desktop_action_cancel)
            val clearLabel = stringResource(MR.strings.clear_all)
            DesktopModalCard(
                title = confirmTitle,
                onDismiss = { showClearHistoryConfirm = false },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    Text(
                        text = cancelLabel,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable { showClearHistoryConfirm = false }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                    Text(
                        text = clearLabel,
                        color = MiuixTheme.colorScheme.error,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        modifier = Modifier
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable {
                                showClearHistoryConfirm = false
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
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }

        if (showBatchDeleteConfirm) {
            val confirmTitle = stringResource(MR.strings.desktop_delete_selected, multiSelection.gids.size)
            val cancelLabel = stringResource(MR.strings.desktop_action_cancel)
            val deleteLabel = stringResource(MR.strings.delete)
            DesktopModalCard(
                title = confirmTitle,
                onDismiss = { showBatchDeleteConfirm = false },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    Text(
                        text = cancelLabel,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable { showBatchDeleteConfirm = false }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                    Text(
                        text = deleteLabel,
                        color = MiuixTheme.colorScheme.error,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        modifier = Modifier
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable {
                                showBatchDeleteConfirm = false
                                coroutineScope.launch {
                                    val gids = multiSelection.gids.toList()
                                    withContext(Dispatchers.IO) {
                                        when (currentTab) {
                                            LibraryTab.History -> gids.forEach {
                                                DesktopDatabase.eh.historyDao().deleteByKey(it)
                                            }
                                            LibraryTab.Favorites -> gids.forEach {
                                                DesktopDatabase.eh.localFavoritesDao().deleteByKey(it)
                                            }
                                            else -> return@withContext
                                        }
                                    }
                                    multiSelection.clear()
                                    selected = null
                                    showNotification(confirmTitle)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun OpenGalleryDialog(
    onDismiss: () -> Unit,
    onOpen: (GalleryParsedTarget) -> Unit,
) {
    val inputState = rememberTextFieldState()
    val input = inputState.text.toString()
    // 输入变化即重置提交错误态（remember(input) 重新初始化）
    var attempted by remember(input) { mutableStateOf(false) }
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
        TextField(
            state = inputState,
            label = hint,
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            onKeyboardAction = {
                if (target != null) onOpen(target) else attempted = true
            },
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
    onOpenReader: (() -> Unit)? = null,
) {
    val detailScrollState = rememberScrollState()
    // 分享摘要栏目标签包：generateShareSummary 无组合语境，经注入走 i18n
    val shareSummaryLabels = ShareSummaryLabels(
        url = stringResource(MR.strings.key_url),
        rating = stringResource(MR.strings.key_rating),
        pages = stringResource(MR.strings.key_pages),
        category = stringResource(MR.strings.key_category),
        tags = stringResource(MR.strings.desktop_share_tags),
        none = stringResource(MR.strings.desktop_share_none),
    )
    // 详情页单次抓取：预览与评论区共享同一份解析结果（避免两个区块重复拉取整页）
    var detailExtras by remember(gallery.gid) { mutableStateOf<GalleryDetailParser.Result?>(null) }
    var extrasLoadFailed by remember(gallery.gid) { mutableStateOf(false) }
    var extrasReloadKey by remember(gallery.gid) { mutableIntStateOf(0) }
    LaunchedEffect(gallery.gid, extrasReloadKey) {
        extrasLoadFailed = false
        runCatching {
            withContext(Dispatchers.IO) {
                val response = desktopGet(galleryWebUrl(gallery.gid, gallery.token))
                val buffer = response.toByteBuffer() ?: error("HTTP ${response.status}")
                GalleryDetailParser.parse(buffer)
            }
        }.onSuccess { result ->
            detailExtras = result
        }.onFailure {
            extrasLoadFailed = true
        }
    }
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
                        .clip(SquircleShape(8.dp))
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
                                .background(MiuixTheme.colorScheme.surfaceContainerHighest)
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
            DetailRow(label = stringResource(MR.strings.key_category), value = DesktopCategories.displayName(gallery.category), onCopy = onCopy)
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
            if (onOpenReader != null) {
                DetailRow(
                    label = stringResource(MR.strings.menu_read),
                    value = stringResource(MR.strings.menu_read_hint),
                    onCopy = onCopy,
                    actionText = stringResource(MR.strings.menu_read),
                    onAction = onOpenReader,
                )
            }
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
                        categoryName = DesktopCategories.displayName(gallery.category),
                        tags = gallery.simpleTags?.toList(),
                        labels = shareSummaryLabels,
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
                                                .clip(SquircleShape(4.dp))
                                                .background(MiuixTheme.colorScheme.surfaceContainerHighest)
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
            DesktopPreviewsSection(previewList = detailExtras?.detail?.previewList)
            DesktopCommentsSection(
                gallery = gallery,
                comments = detailExtras?.detail?.comments?.comments,
                loadFailed = extrasLoadFailed,
                onRetry = { extrasReloadKey += 1 },
                apiUid = detailExtras?.detail?.apiUid ?: -1L,
                apiKey = detailExtras?.detail?.apiKey,
            )
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
                .clip(SquircleShape(24.dp))
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
