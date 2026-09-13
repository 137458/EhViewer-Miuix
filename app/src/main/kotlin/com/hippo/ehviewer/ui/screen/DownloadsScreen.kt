package com.hippo.ehviewer.ui.screen

import android.content.Context
import android.view.ViewConfiguration
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.fork.SwipeToDismissBox
import androidx.compose.material3.fork.SwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import arrow.core.partially1
import com.ehviewer.core.database.model.DownloadInfo
import com.ehviewer.core.i18n.R
import com.ehviewer.core.model.TagNamespace
import com.ehviewer.core.ui.component.FAB_ANIMATE_TIME
import com.ehviewer.core.ui.component.FabLayout
import com.ehviewer.core.ui.component.FastScrollLazyColumn
import com.ehviewer.core.ui.component.FastScrollLazyVerticalStaggeredGrid
import com.ehviewer.core.ui.component.LocalSideSheetState
import com.ehviewer.core.ui.component.ProvideSideSheetContent
import com.ehviewer.core.ui.component.SquircleShape
import com.ehviewer.core.ui.icons.EhIcons
import com.ehviewer.core.ui.icons.big.Download
import com.ehviewer.core.ui.icons.filled.Shuffle
import com.ehviewer.core.ui.util.HapticFeedbackType
import com.ehviewer.core.ui.util.asyncState
import com.ehviewer.core.ui.util.ifTrueThen
import com.ehviewer.core.ui.util.rememberHapticFeedback
import com.ehviewer.core.ui.util.rememberInVM
import com.ehviewer.core.ui.util.takeAndClear
import com.ehviewer.core.ui.util.thenIf
import com.ehviewer.core.util.launch
import com.ehviewer.core.util.launchIO
import com.ehviewer.core.util.mapToLongArray
import com.ehviewer.core.util.onEachLatest
import com.ehviewer.core.util.withNonCancellableContext
import com.ehviewer.core.util.withUIContext
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.asMutableState
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.download.DownloadService
import com.hippo.ehviewer.download.DownloadsFilterMode
import com.hippo.ehviewer.download.SortMode
import com.hippo.ehviewer.ui.DrawerHandle
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.confirmRemoveDownloadRange
import com.hippo.ehviewer.ui.main.DownloadCard
import com.hippo.ehviewer.ui.main.GalleryInfoGridItem
import com.hippo.ehviewer.ui.navToReader
import com.hippo.ehviewer.ui.showMoveDownloadLabelList
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.ui.tools.awaitConfirmationOrCancel
import com.hippo.ehviewer.ui.tools.awaitInputText
import com.hippo.ehviewer.ui.tools.awaitSelectAction
import com.hippo.ehviewer.ui.tools.awaitSelectItemWithCheckBox
import com.hippo.ehviewer.ui.tools.awaitSingleChoice
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import moe.tarsin.navigate
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.MoveFile
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.SelectAll
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.menu.WindowIconDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.DownloadsScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    var gridView by Settings.gridView.asMutableState()
    var sortMode by Settings.downloadSortMode.asMutableState()
    val filterMode by Settings.downloadFilterMode.collectAsState { DownloadsFilterMode.from(it) }
    val showProgress by Settings.showReadingProgress.collectAsState()
    var filterState by rememberSerializable { mutableStateOf(DownloadsFilterState(filterMode, Settings.recentDownloadLabel.value)) }
    var invalidateKey by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(true) }
    var searchBarExpanded by rememberSaveable { mutableStateOf(false) }
    var searchBarOffsetY by remember { mutableIntStateOf(0) }
    val animateItems by Settings.animateItems.collectAsState()

    var fabExpanded by remember { mutableStateOf(false) }
    var fabHidden by remember { mutableStateOf(false) }
    val checkedInfoMap = remember { mutableStateMapOf<Long, DownloadInfo>() }
    val selectMode by rememberUpdatedState(checkedInfoMap.isNotEmpty())
    DrawerHandle(!selectMode && !searchBarExpanded)

    val density = LocalDensity.current
    val canTranslate = Settings.showTagTranslations.value && EhTagDatabase.translatable && EhTagDatabase.initialized
    val ehTags = EhTagDatabase.takeIf { canTranslate }
    fun getTranslation(tag: String) = ehTags?.run {
        getTranslation(TagNamespace.Artist.prefix, tag) ?: getTranslation(TagNamespace.Cosplayer.prefix, tag)
    } ?: tag
    val positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold
    val allName = stringResource(R.string.download_all)
    val defaultName = stringResource(R.string.default_download_label_name)
    val unknownName = stringResource(R.string.unknown_artists)
    val emptyLabelName = when (filterMode) {
        DownloadsFilterMode.ARTIST -> unknownName
        DownloadsFilterMode.CUSTOM -> defaultName
    }
    val title = stringResource(
        R.string.scene_download_title,
        with(filterState) {
            when (label) {
                "" -> allName
                null -> emptyLabelName
                else -> if (mode == DownloadsFilterMode.ARTIST) getTranslation(label) else label
            }
        },
    )
    val hint = stringResource(R.string.search_bar_hint, title)
    val list = if (DownloadManager.isInitialized) {
        remember(filterState, invalidateKey) {
            DownloadManager.downloadInfoList.filterTo(mutableStateListOf()) { info ->
                filterState.take(info)
            }.also {
                launch {
                    delay(200)
                    isLoading = false
                }
            }
        }
    } else {
        remember { mutableStateListOf() }
    }

    val newLabel = stringResource(R.string.new_label_title)
    val renameLabel = stringResource(R.string.rename_label_title)
    val labelsStr = stringResource(R.string.download_labels)
    val labelEmpty = stringResource(R.string.label_text_is_empty)
    val defaultInvalid = stringResource(R.string.label_text_is_invalid)
    val labelExists = stringResource(R.string.label_text_exist)
    val downloadsCountGroupByArtist by rememberInVM { EhDB.downloadsCountByArtist }.collectAsState(emptyMap())
    val downloadsCountGroupByLabel by rememberInVM { EhDB.downloadsCountByLabel }.collectAsState(emptyMap())
    val downloadsCount = when (filterMode) {
        DownloadsFilterMode.CUSTOM -> downloadsCountGroupByLabel
        DownloadsFilterMode.ARTIST -> downloadsCountGroupByArtist
    }
    val artistList = remember(downloadsCountGroupByArtist) {
        downloadsCountGroupByArtist.keys.mapNotNull { artist -> artist?.let { it to it } }
    }
    val labelList by remember {
        derivedStateOf {
            DownloadManager.labelList.map { it.id!! to it.label }
        }
    }
    val groupList = when (filterMode) {
        DownloadsFilterMode.CUSTOM -> labelList
        DownloadsFilterMode.ARTIST -> artistList
    }
    val totalCount = remember(downloadsCountGroupByLabel) { downloadsCountGroupByLabel.values.sum() }

    fun switchLabel(label: String?) {
        Settings.recentDownloadLabel.value = label
        filterState = filterState.copy(label = label)
        fabHidden = false
    }

    LaunchedEffect(filterState) {
        searchBarOffsetY = 0
    }

    ProvideSideSheetContent { drawerState ->
        fun closeSheet() = launch { drawerState.close() }
        SmallTopAppBar(
            title = labelsStr,
            color = Color.Transparent,
            defaultWindowInsetsPadding = false,
            actions = {
                if (DownloadsFilterMode.CUSTOM == filterMode) {
                    IconButton(
                        onClick = {
                            launch {
                                val text = awaitInputText(title = newLabel, hint = labelsStr) { text ->
                                    when {
                                        text.isBlank() -> raise(labelEmpty)
                                        text == defaultName -> raise(defaultInvalid)
                                        DownloadManager.containLabel(text) -> raise(labelExists)
                                    }
                                }
                                DownloadManager.addLabel(text)
                            }
                        },
                    ) {
                        Icon(imageVector = MiuixIcons.Add, contentDescription = null)
                    }
                    val letMeSelect = stringResource(R.string.let_me_select)
                    IconButton(
                        onClick = {
                            launch {
                                val selected = if (!Settings.hasDefaultDownloadLabel) {
                                    0
                                } else {
                                    DownloadManager.labelList.indexOfFirst { it.label == Settings.defaultDownloadLabel } + 2
                                }
                                awaitSelectAction(R.string.default_download_label, selected) {
                                    onSelect(letMeSelect) {
                                        Settings.hasDefaultDownloadLabel = false
                                    }
                                    onSelect(defaultName) {
                                        Settings.hasDefaultDownloadLabel = true
                                        Settings.defaultDownloadLabel = null
                                    }
                                    DownloadManager.labelList.forEach { (label) ->
                                        onSelect(label) {
                                            Settings.hasDefaultDownloadLabel = true
                                            Settings.defaultDownloadLabel = label
                                        }
                                    }
                                }()
                            }
                        },
                    ) {
                        Icon(imageVector = MiuixIcons.Download, contentDescription = null)
                    }
                }
                val custom = stringResource(R.string.select_grouping_mode_custom)
                val artist = stringResource(R.string.select_grouping_mode_artist)
                IconButton(
                    onClick = {
                        launch {
                            awaitSelectAction(R.string.select_grouping_mode) {
                                val select = { mode: DownloadsFilterMode ->
                                    filterState = filterState.copy(mode = mode, label = "")
                                    Settings.downloadFilterMode.value = mode.flag
                                    Settings.recentDownloadLabel.value = ""
                                }
                                onSelect(custom) { select(DownloadsFilterMode.CUSTOM) }
                                onSelect(artist) { select(DownloadsFilterMode.ARTIST) }
                            }()
                        }
                    },
                ) {
                    Icon(imageVector = MiuixIcons.Settings, contentDescription = null)
                }
            },
        )

        val dialogState by rememberUpdatedState(contextOf<DialogState>())
        val labelsListState = rememberLazyListState()
        val editEnable = DownloadsFilterMode.CUSTOM == filterMode
        val hapticFeedback = rememberHapticFeedback()
        val reorderableLabelState = rememberReorderableLazyListState(labelsListState) { from, to ->
            val fromPosition = from.index - 2
            val toPosition = to.index - 2
            DownloadManager.labelList.apply { add(toPosition, removeAt(fromPosition)) }
            hapticFeedback.performHapticFeedback(HapticFeedbackType.MOVE)
        }
        var fromIndex by remember { mutableIntStateOf(-1) }
        FastScrollLazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            state = labelsListState,
            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues(),
        ) {
            item {
                val selected = filterState.label == ""
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .clip(SquircleShape(8.dp))
                        .background(
                            if (selected) {
                                MiuixTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            },
                        )
                        .clickable(role = Role.RadioButton) {
                            switchLabel("")
                            closeSheet()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$allName [$totalCount]",
                        color = if (selected) MiuixTheme.colorScheme.onPrimaryContainer else MiuixTheme.colorScheme.onSurface,
                        style = MiuixTheme.textStyles.body1,
                    )
                }
            }
            item {
                val selected = filterState.label == null
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .clip(SquircleShape(8.dp))
                        .background(
                            if (selected) {
                                MiuixTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            },
                        )
                        .clickable(role = Role.RadioButton) {
                            switchLabel(null)
                            closeSheet()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$emptyLabelName [${downloadsCount.getOrDefault(null, 0)}]",
                        color = if (selected) MiuixTheme.colorScheme.onPrimaryContainer else MiuixTheme.colorScheme.onSurface,
                        style = MiuixTheme.textStyles.body1,
                    )
                }
            }

            itemsIndexed(groupList, key = { _, (id) -> id }) { itemIndex, (id, label) ->
                val index by rememberUpdatedState(itemIndex)
                val item by rememberUpdatedState(label)
                ReorderableItem(
                    reorderableLabelState,
                    id,
                    enabled = editEnable,
                    animateItemModifier = Modifier.thenIf(animateItems) { animateItem() },
                ) { isDragging ->
                    // Not using rememberSwipeToDismissBoxState to prevent LazyColumn from reusing it
                    // SQLite may reuse ROWIDs from previously deleted rows so they'll have the same key
                    val dismissState = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, positionalThreshold) }
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Delete,
                                    contentDescription = stringResource(id = R.string.delete),
                                    tint = MiuixTheme.colorScheme.error,
                                    modifier = Modifier.padding(end = 20.dp),
                                )
                            }
                        },
                        enableDismissFromStartToEnd = false,
                        gesturesEnabled = editEnable,
                        onDismiss = {
                            dialogState.runCatching {
                                awaitConfirmationOrCancel(confirmText = R.string.delete) {
                                    Text(text = stringResource(R.string.delete_label, item))
                                }
                            }.onSuccess {
                                DownloadManager.deleteLabel(item)
                                when (filterState.label) {
                                    item -> switchLabel("")
                                    null -> invalidateKey = !invalidateKey
                                }
                            }.onFailure {
                                dismissState.reset()
                            }
                        },
                    ) {
                        val selected = filterState.label == item
                        val name = if (filterMode == DownloadsFilterMode.ARTIST) getTranslation(label) else label
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .clip(SquircleShape(8.dp))
                                .background(
                                    if (selected) {
                                        MiuixTheme.colorScheme.primaryContainer
                                    } else if (isDragging) {
                                        MiuixTheme.colorScheme.surfaceContainer
                                    } else {
                                        Color.Transparent
                                    },
                                )
                                .clickable(role = Role.RadioButton) {
                                    switchLabel(item)
                                    closeSheet()
                                }
                                .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "$name [${downloadsCount.getOrDefault(item, 0)}]",
                                color = if (selected) MiuixTheme.colorScheme.onPrimaryContainer else MiuixTheme.colorScheme.onSurface,
                                style = MiuixTheme.textStyles.body1,
                                modifier = Modifier.weight(1f),
                            )
                            if (editEnable) {
                                IconButton(
                                    onClick = {
                                        launch {
                                            val new = awaitInputText(initial = item, title = renameLabel, hint = labelsStr) { text ->
                                                when {
                                                    text.isBlank() -> raise(labelEmpty)
                                                    text == defaultName -> raise(defaultInvalid)
                                                    DownloadManager.containLabel(text) -> raise(labelExists)
                                                }
                                            }
                                            DownloadManager.renameLabel(item, new)
                                            if (filterState.label == item) {
                                                switchLabel(new)
                                            }
                                        }
                                    },
                                ) {
                                    Icon(imageVector = MiuixIcons.Edit, contentDescription = null)
                                }
                                IconButton(
                                    onClick = {},
                                    modifier = Modifier.draggableHandle(
                                        onDragStarted = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.START)
                                            fromIndex = index
                                        },
                                        onDragStopped = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.END)
                                            if (fromIndex != -1) {
                                                if (fromIndex != index) {
                                                    val range = if (fromIndex < index) fromIndex..index else index..fromIndex
                                                    val toUpdate = DownloadManager.labelList.slice(range)
                                                    toUpdate.zip(range).forEach { it.first.position = it.second }
                                                    launchIO { EhDB.updateDownloadLabel(toUpdate) }
                                                }
                                                fromIndex = -1
                                            }
                                        },
                                    ),
                                ) {
                                    Icon(imageVector = MiuixIcons.Sort, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val searchFieldState = rememberTextFieldState()
    class DownloadLabelSuggestion(private val label: String) : Suggestion() {
        override val keyword = LABEL_PREFIX + label
        override fun onClick() {
            searchFieldState.setTextAndPlaceCursorAtEnd(keyword)
            searchBarExpanded = false
            switchLabel(label)
        }
    }
    SearchBarScreen(
        onApplySearch = { filterState = filterState.copy(keyword = it) },
        expanded = searchBarExpanded,
        onExpandedChange = {
            searchBarExpanded = it
            fabHidden = it
            if (it) checkedInfoMap.clear()
        },
        title = title,
        searchFieldHint = hint,
        searchFieldState = searchFieldState,
        suggestionProvider = { query ->
            val label = query.substringAfter(LABEL_PREFIX, "")
            if (label.isEmpty()) {
                emptyList()
            } else {
                EhDB.searchDownloadLabel(label, 10).map(::DownloadLabelSuggestion)
            }
        },
        searchBarOffsetY = { searchBarOffsetY },
        trailingIcon = {
            val sideSheetState = LocalSideSheetState.current
            IconButton(onClick = { gridView = !gridView }) {
                val icon = if (gridView) MiuixIcons.ListView else MiuixIcons.GridView
                Icon(imageVector = icon, contentDescription = null)
            }
            val labelsStr = stringResource(id = R.string.download_labels)
            val startAllStr = stringResource(id = R.string.download_start_all)
            val stopAllStr = stringResource(id = R.string.download_stop_all)
            val resetProgressStr = stringResource(id = R.string.download_reset_reading_progress)
            val resetProgressMsg = stringResource(id = R.string.reset_reading_progress_message)
            val startAllReversedStr = stringResource(id = R.string.download_start_all_reversed)

            val menuEntry = remember(sideSheetState, list) {
                DropdownEntry(
                    items = listOf(
                        DropdownItem(
                            text = labelsStr,
                            onClick = { launch { sideSheetState.open() } },
                        ),
                        DropdownItem(
                            text = startAllStr,
                            onClick = { DownloadService.startService(DownloadService.ACTION_START_ALL) },
                        ),
                        DropdownItem(
                            text = stopAllStr,
                            onClick = { launchIO { DownloadManager.stopAllDownload() } },
                        ),
                        DropdownItem(
                            text = resetProgressStr,
                            onClick = {
                                launchIO {
                                    awaitConfirmationOrCancel(
                                        confirmText = android.R.string.ok,
                                        dismissText = android.R.string.cancel,
                                    ) {
                                        Text(text = resetProgressMsg)
                                    }
                                    withNonCancellableContext {
                                        DownloadManager.resetAllReadingProgress()
                                    }
                                }
                            },
                        ),
                        DropdownItem(
                            text = startAllReversedStr,
                            onClick = {
                                val gidList = list.filter { it.state != DownloadInfo.STATE_FINISH }.asReversed().mapToLongArray(DownloadInfo::gid)
                                DownloadService.startRangeDownload(gidList)
                            },
                        ),
                    ),
                )
            }
            WindowIconDropdownMenu(entry = menuEntry) {
                Icon(imageVector = MiuixIcons.More, contentDescription = null)
            }
        },
    ) { contentPadding ->
        val height by collectListThumbSizeAsState()
        val realPadding = contentPadding + PaddingValues(dimensionResource(id = com.hippo.ehviewer.R.dimen.gallery_list_margin_h), dimensionResource(id = com.hippo.ehviewer.R.dimen.gallery_list_margin_v))
        val searchBarConnection = remember {
            val slop = ViewConfiguration.get(contextOf<Context>()).scaledTouchSlop
            val topPaddingPx = with(density) { contentPadding.calculateTopPadding().roundToPx() }
            object : NestedScrollConnection {
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    val dy = -consumed.y
                    if (dy >= slop) {
                        fabHidden = true
                    } else if (dy <= -slop / 2) {
                        fabHidden = false
                    }
                    searchBarOffsetY = (searchBarOffsetY - dy).roundToInt().coerceIn(-topPaddingPx, 0)
                    return Offset.Zero // We never consume it
                }
            }
        }

        fun onItemClick(info: DownloadInfo) {
            launchIO { EhDB.putHistoryInfo(info) }
            navToReader(info.galleryInfo)
        }

        Crossfade(targetState = gridView, label = "Downloads") { showGridView ->
            if (showGridView) {
                val gridInterval = dimensionResource(com.hippo.ehviewer.R.dimen.gallery_grid_interval)
                val thumbColumns by Settings.thumbColumns.collectAsState()
                FastScrollLazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(thumbColumns),
                    modifier = Modifier.nestedScroll(searchBarConnection).fillMaxSize(),
                    contentPadding = realPadding,
                    verticalItemSpacing = gridInterval,
                    horizontalArrangement = Arrangement.spacedBy(gridInterval),
                ) {
                    items(list, key = { it.gid }) { info ->
                        GalleryInfoGridItem(
                            onClick = ::onItemClick.partially1(info),
                            onLongClick = { navigate(info.galleryInfo.asDst()) },
                            info = info,
                            modifier = Modifier.thenIf(animateItems) { animateItem() },
                            showLanguage = false,
                            showProgress = showProgress,
                        )
                    }
                }
            } else {
                FastScrollLazyColumn(
                    modifier = Modifier.nestedScroll(searchBarConnection).fillMaxSize(),
                    contentPadding = realPadding,
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(com.hippo.ehviewer.R.dimen.gallery_list_interval)),
                ) {
                    items(list, key = { it.gid }) { info ->
                        val checked = info.gid in checkedInfoMap
                        CheckableItem(
                            checked = checked,
                            modifier = Modifier.thenIf(animateItems) { animateItem() },
                        ) { interactionSource ->
                            DownloadCard(
                                onClick = {
                                    if (selectMode) {
                                        if (checked) {
                                            checkedInfoMap.remove(info.gid)
                                        } else {
                                            checkedInfoMap[info.gid] = info
                                        }
                                    } else {
                                        onItemClick(info)
                                    }
                                },
                                onThumbClick = {
                                    navigate(info.galleryInfo.asDst())
                                },
                                onLongClick = {
                                    checkedInfoMap[info.gid] = info
                                },
                                onStart = {
                                    DownloadService.startDownload(info.galleryInfo)
                                },
                                onStop = { launchIO { DownloadManager.stopDownload(info.gid) } },
                                info = info,
                                selectMode = selectMode,
                                showProgress = showProgress,
                                modifier = Modifier.height(height),
                                interactionSource = interactionSource,
                            )
                        }
                    }
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().background(MiuixTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                InfiniteProgressIndicator()
            }
        } else if (list.isEmpty()) {
            Column(
                modifier = Modifier.padding(realPadding).fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = EhIcons.Big.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.padding(16.dp),
                    tint = MiuixTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(id = R.string.no_download_info),
                    style = MiuixTheme.textStyles.title2,
                )
            }
        }
    }

    val hideFab by asyncState(
        produce = { fabHidden },
        transform = {
            onEachLatest { hide ->
                if (!hide) delay(FAB_ANIMATE_TIME.toLong())
            }
        },
    )

    FabLayout(
        hidden = hideFab && !selectMode,
        expanded = fabExpanded || selectMode,
        onExpandChanged = {
            fabExpanded = it
            checkedInfoMap.clear()
        },
        autoCancel = !selectMode,
    ) {
        if (!selectMode) {
            onClick(EhIcons.Default.Shuffle) {
                if (list.isNotEmpty()) {
                    withUIContext { navToReader(list.random().galleryInfo) }
                }
            }
            onClick(MiuixIcons.Sort) {
                val oldMode = SortMode.from(sortMode)
                val sortModes = contextOf<Context>().resources.getStringArray(com.hippo.ehviewer.R.array.download_sort_modes).toList()
                val (selected, checked) = awaitSelectItemWithCheckBox(
                    sortModes,
                    R.string.sort_by,
                    R.string.group_by_download_label,
                    SortMode.All.indexOfFirst { it.field == oldMode.field && it.order == oldMode.order },
                    oldMode.groupByDownloadLabel,
                )
                val mode = SortMode.All[selected].copy(groupByDownloadLabel = checked)
                if (mode != oldMode) {
                    sortMode = mode.flag
                    isLoading = true
                    DownloadManager.sortDownloads(mode)
                    invalidateKey = !invalidateKey
                }
            }
            onClick(MiuixIcons.Filter) {
                val downloadStates = contextOf<Context>().resources.getStringArray(com.hippo.ehviewer.R.array.download_state).toList()
                val state = awaitSingleChoice(
                    downloadStates,
                    filterState.state + 1,
                    R.string.download_filter,
                ) - 1
                filterState = filterState.copy(state = state)
            }
        } else {
            onClick(MiuixIcons.SelectAll, autoClose = false) {
                val info = list.associateBy { it.gid }
                checkedInfoMap.putAll(info)
            }
            onClick(MiuixIcons.Play) {
                val gidList = checkedInfoMap.takeAndClear().mapToLongArray(DownloadInfo::gid)
                DownloadService.startRangeDownload(gidList)
            }
            onClick(MiuixIcons.Pause) {
                val gidList = checkedInfoMap.takeAndClear().mapToLongArray(DownloadInfo::gid)
                DownloadManager.stopRangeDownload(gidList)
            }
            onClick(MiuixIcons.Delete) {
                val infoList = checkedInfoMap.takeAndClear()
                confirmRemoveDownloadRange(infoList)
                list.removeAll(infoList)
            }
            onClick(MiuixIcons.MoveFile) {
                val infoList = checkedInfoMap.takeAndClear()
                val toLabel = showMoveDownloadLabelList(infoList)
                with(filterState) {
                    if (label != "" && label != toLabel) {
                        list.removeAll(infoList)
                    }
                }
            }
        }
    }
}

private const val LABEL_PREFIX = "label:"
