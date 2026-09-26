package com.hippo.ehviewer.ui.screen

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import arrow.core.partially1
import arrow.fx.coroutines.parMap
import arrow.fx.coroutines.parZip
import com.ehviewer.core.data.model.asGalleryDetail
import com.ehviewer.core.data.model.findBaseInfo
import com.ehviewer.core.database.model.DownloadInfo
import com.ehviewer.core.database.model.Filter
import com.ehviewer.core.database.model.FilterMode
import com.ehviewer.core.i18n.R
import com.ehviewer.core.model.GalleryComment
import com.ehviewer.core.model.GalleryDetail
import com.ehviewer.core.model.GalleryInfo
import com.ehviewer.core.model.GalleryInfo.Companion.NOT_FAVORITED
import com.ehviewer.core.model.GalleryPreview
import com.ehviewer.core.model.GalleryTagGroup
import com.ehviewer.core.model.TagNamespace
import com.ehviewer.core.model.V2GalleryPreview
import com.ehviewer.core.model.VoteStatus
import com.ehviewer.core.ui.component.CrystalCard
import com.ehviewer.core.ui.component.FastScrollLazyVerticalGrid
import com.ehviewer.core.ui.component.FilledTertiaryIconButton
import com.ehviewer.core.ui.component.FilledTertiaryIconToggleButton
import com.ehviewer.core.ui.component.GalleryDetailRating
import com.ehviewer.core.ui.component.GalleryRatingBar
import com.ehviewer.core.ui.icons.EhIcons
import com.ehviewer.core.ui.icons.filled.Magnet
import com.ehviewer.core.ui.util.LocalWindowLayout
import com.ehviewer.core.ui.util.LocalWindowSizeClass
import com.ehviewer.core.ui.util.TransitionsVisibilityScope
import com.ehviewer.core.ui.util.WindowLayout
import com.ehviewer.core.ui.util.flattenForEach
import com.ehviewer.core.ui.util.isExpanded
import com.ehviewer.core.ui.util.rememberInVM
import com.ehviewer.core.util.async
import com.ehviewer.core.util.launch
import com.ehviewer.core.util.launchIO
import com.ehviewer.core.util.launchUI
import com.ehviewer.core.util.logcat
import com.ehviewer.core.util.withIOContext
import com.ehviewer.core.util.withUIContext
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhFilter.remember
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.client.exception.NoHathClientException
import com.hippo.ehviewer.coil.PrefetchAround
import com.hippo.ehviewer.coil.justDownload
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.ktbuilder.execute
import com.hippo.ehviewer.ktbuilder.executeIn
import com.hippo.ehviewer.ktbuilder.imageRequest
import com.hippo.ehviewer.ui.GalleryInfoBottomSheet
import com.hippo.ehviewer.ui.MainActivity
import com.hippo.ehviewer.ui.confirmRemoveDownload
import com.hippo.ehviewer.ui.collectConfiguredThumbColumns
import com.hippo.ehviewer.ui.destinations.GalleryCommentsScreenDestination
import com.hippo.ehviewer.ui.getFavoriteIcon
import com.hippo.ehviewer.ui.jumpToReaderByPage
import com.hippo.ehviewer.ui.main.ArchiveList
import com.hippo.ehviewer.ui.main.EhPreviewItem
import com.hippo.ehviewer.ui.main.GalleryCommentCard
import com.hippo.ehviewer.ui.main.GalleryDetailErrorTip
import com.hippo.ehviewer.ui.main.GalleryDetailHeaderCard
import com.hippo.ehviewer.ui.main.GalleryTags
import com.hippo.ehviewer.ui.main.TorrentList
import com.hippo.ehviewer.ui.modifyFavorites
import com.hippo.ehviewer.ui.navToReader
import com.hippo.ehviewer.ui.openBrowser
import com.hippo.ehviewer.ui.startDownload
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.ui.tools.awaitConfirmationOrCancel
import com.hippo.ehviewer.ui.tools.awaitResult
import com.hippo.ehviewer.ui.tools.awaitSelectAction
import com.hippo.ehviewer.ui.tools.awaitSelectItem
import com.hippo.ehviewer.ui.tools.dialog
import com.hippo.ehviewer.ui.tools.foldToLoadResult
import com.hippo.ehviewer.ui.tools.getClippedRefreshKey
import com.hippo.ehviewer.ui.tools.getLimit
import com.hippo.ehviewer.ui.tools.getOffset
import com.hippo.ehviewer.ui.tools.showNoButton
import com.hippo.ehviewer.util.FavouriteStatusRouter
import com.hippo.ehviewer.util.addTextToClipboard
import com.hippo.ehviewer.util.bgWork
import com.hippo.ehviewer.util.displayString
import com.hippo.ehviewer.util.sha1
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import io.ktor.http.encodeURLParameter
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import moe.tarsin.coroutines.runSuspendCatching
import moe.tarsin.coroutines.runSwallowingWithUI
import moe.tarsin.navigate
import moe.tarsin.snackbar
import moe.tarsin.string
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/** 画廊详情布局模式：跟随方向。 */
const val GALLERY_DETAIL_LAYOUT_AUTO = 0

/** 画廊详情布局模式：强制单列。 */
const val GALLERY_DETAIL_LAYOUT_SINGLE_COLUMN = 1

/** 画廊详情布局模式：强制双栏。 */
const val GALLERY_DETAIL_LAYOUT_DUAL_PANE = 2

/** 双栏中单侧的最小宽度，避免任一栏被拖得不可用（借鉴 PixEz illust_row_page 的 atLeastWidth）。 */
private const val DUAL_PANE_MIN_WIDTH_DP = 320

/** 双栏分隔条的触摸区宽度。 */
private const val DUAL_PANE_DIVIDER_WIDTH_DP = 28

/** 分隔位置（百分比）的持久化范围。 */
private val DUAL_PANE_SPLIT_PERCENT_RANGE = 20..80

@Composable
context(_: CoroutineScope, _: DestinationsNavigator, _: DialogState, _: MainActivity, _: SnackbarHostState, _: SharedTransitionScope, _: TransitionsVisibilityScope)
fun GalleryDetailContent(
    galleryInfo: GalleryInfo,
    contentPadding: PaddingValues,
    getDetailError: String,
    onRetry: () -> Unit,
    voteTag: VoteTag,
    modifier: Modifier,
) {
    val keylineMargin = dimensionResource(com.hippo.ehviewer.R.dimen.keyline_margin)
    val galleryDetail = galleryInfo.asGalleryDetail()
    val windowSizeClass = LocalWindowSizeClass.current
    val thumbColumns = collectConfiguredThumbColumns()
    val readText = stringResource(R.string.read)
    val startPage by rememberInVM {
        EhDB.getReadProgressFlow(galleryInfo.gid)
    }.collectAsState(0)
    val readButtonText = if (startPage == 0) {
        readText
    } else {
        stringResource(R.string.read_from, startPage + 1)
    }
    val downloadState by DownloadManager.collectDownloadState(galleryInfo.gid)
    val downloadButtonText = when (downloadState) {
        DownloadInfo.STATE_INVALID -> stringResource(R.string.download)
        DownloadInfo.STATE_NONE -> stringResource(R.string.download_state_none)
        DownloadInfo.STATE_WAIT -> stringResource(R.string.download_state_wait)
        DownloadInfo.STATE_DOWNLOAD -> stringResource(R.string.download_state_downloading)
        DownloadInfo.STATE_FINISH -> stringResource(R.string.download_state_downloaded)
        DownloadInfo.STATE_FAILED -> stringResource(R.string.download_state_failed)
        else -> error("Invalid DownloadState!!!")
    }
    fun onReadButtonClick() {
        if (galleryDetail != null || downloadState != DownloadInfo.STATE_INVALID) {
            navToReader(galleryInfo.findBaseInfo(), startPage)
        }
    }
    fun onCategoryChipClick() {
        val category = galleryInfo.category
        if (category == EhUtils.NONE || category == EhUtils.PRIVATE || category == EhUtils.UNKNOWN) {
            return
        }
        navigate(ListUrlBuilder(category = category).asDst())
    }
    fun onUploaderChipClick(galleryInfo: GalleryInfo) {
        val uploader = galleryInfo.uploader
        val disowned = uploader == "(Disowned)"
        if (uploader.isNullOrEmpty() || disowned) {
            return
        }
        navigate(ListUrlBuilder(mode = ListUrlBuilder.MODE_UPLOADER, keyword = uploader).asDst())
    }

    fun onGalleryInfoCardClick() {
        galleryDetail ?: return
        launch {
            dialog { cont ->
                WindowBottomSheet(
                    show = true,
                    onDismissRequest = { cont.cancel() },
                ) {
                    GalleryInfoBottomSheet(galleryDetail)
                }
            }
        }
    }

    suspend fun searchGalleryCover(info: GalleryInfo) {
        val key = info.thumbKey
        if (key.isNullOrEmpty()) {
            snackbar(string(R.string.error_cant_save_image))
            return
        }
        val hash = withIOContext {
            var snapshot = EhApplication.thumbCache.openSnapshot(key)
            if (snapshot == null) {
                imageRequest(info).execute()
                snapshot = EhApplication.thumbCache.openSnapshot(key)
            }
            snapshot?.use { it.data.sha1() }
        }
        if (hash != null) {
            withUIContext {
                navigate(
                    ListUrlBuilder(
                        mode = ListUrlBuilder.MODE_IMAGE_SEARCH,
                        hash = hash,
                    ).asDst(),
                )
            }
        } else {
            snackbar(string(R.string.error_cant_save_image))
        }
    }

    fun onCoverClick() {
        launch {
            dialog { cont ->
                WindowBottomSheet(
                    show = true,
                    onDismissRequest = { cont.cancel() },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable(role = Role.Button) {
                                    cont.resume(Unit)
                                    launchIO {
                                        searchGalleryCover(galleryInfo)
                                    }
                                }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(imageVector = MiuixIcons.Search, contentDescription = null, tint = MiuixTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.size(32.dp))
                            Text(text = stringResource(id = R.string.image_search), style = MiuixTheme.textStyles.body1)
                        }
                    }
                }
            }
        }
    }

    val filterAdded = stringResource(R.string.filter_added)
    fun showFilterUploaderDialog(galleryInfo: GalleryInfo) {
        val uploader = galleryInfo.uploader
        val disowned = uploader == "(Disowned)"
        if (uploader.isNullOrEmpty() || disowned) {
            return
        }
        launchIO {
            awaitConfirmationOrCancel {
                Text(text = stringResource(R.string.filter_the_uploader, uploader))
            }
            Filter(FilterMode.UPLOADER, uploader).remember()
            snackbar(filterAdded)
        }
    }
    fun onDownloadButtonClick() {
        galleryDetail ?: return
        if (DownloadManager.getDownloadState(galleryDetail.gid) == DownloadInfo.STATE_INVALID) {
            launchUI { startDownload(false, galleryDetail.galleryInfo) }
        } else {
            launch { confirmRemoveDownload(galleryDetail) }
        }
    }

    val previews = galleryDetail?.collectPreviewItems()
    val windowLayout = LocalWindowLayout.current
    val detailLayoutMode by Settings.galleryDetailLayout.collectAsState()
    val useDualPane = when (detailLayoutMode) {
        GALLERY_DETAIL_LAYOUT_SINGLE_COLUMN -> false
        GALLERY_DETAIL_LAYOUT_DUAL_PANE -> true
        else -> windowLayout.isLargeLandscape
    }
    when {
        useDualPane -> BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val stripSpacing = dimensionResource(id = com.hippo.ehviewer.R.dimen.strip_item_padding)
            val verticalSpacing = dimensionResource(id = com.hippo.ehviewer.R.dimen.strip_item_padding_v)
            val density = LocalDensity.current
            val totalWidthPx = with(density) { maxWidth.toPx() }
            val minPaneFraction = if (maxWidth > DUAL_PANE_MIN_WIDTH_DP.dp * 2) {
                DUAL_PANE_MIN_WIDTH_DP.dp / maxWidth
            } else {
                0.5f
            }
            var splitFraction by remember { mutableFloatStateOf(Settings.galleryDetailSplitPercent.value / 100f) }
            val paneFraction = splitFraction.coerceIn(minPaneFraction, 1f - minPaneFraction)
            Row(modifier = Modifier.fillMaxSize()) {
                BoxWithConstraints(modifier = Modifier.weight(paneFraction).fillMaxHeight()) {
                    val leftWidthDp = maxWidth.value.roundToInt().coerceAtLeast(1)
                    FastScrollLazyVerticalGrid(
                        columns = GridCells.Fixed(WindowLayout.thumbGridColumns(leftWidthDp, thumbColumns)),
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize().padding(horizontal = keylineMargin),
                        horizontalArrangement = Arrangement.spacedBy(stripSpacing),
                        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                    ) {
                        if (galleryDetail != null && previews != null) {
                            galleryPreview(galleryDetail, previews) { navToReader(galleryDetail.galleryInfo, it) }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(DUAL_PANE_DIVIDER_WIDTH_DP.dp)
                        .draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState { delta ->
                                if (totalWidthPx > 0f) splitFraction += delta / totalWidthPx
                            },
                            onDragStopped = {
                                Settings.galleryDetailSplitPercent.value =
                                    (paneFraction * 100).roundToInt().coerceIn(DUAL_PANE_SPLIT_PERCENT_RANGE)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f - paneFraction)
                        .fillMaxHeight()
                        .padding(contentPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Column(modifier = Modifier.padding(horizontal = keylineMargin)) {
                        GalleryDetailHeaderCard(
                            info = galleryInfo,
                            onInfoCardClick = ::onGalleryInfoCardClick,
                            onUploaderChipClick = ::onUploaderChipClick.partially1(galleryInfo),
                            onBlockUploaderIconClick = ::showFilterUploaderDialog.partially1(galleryInfo),
                            onCategoryChipClick = ::onCategoryChipClick,
                            onCoverClick = ::onCoverClick,
                            modifier = Modifier.fillMaxWidth().padding(vertical = keylineMargin),
                        )
                        Row {
                            Button(
                                onClick = ::onDownloadButtonClick,
                                colors = ButtonDefaults.buttonColors(),
                                modifier = Modifier.padding(horizontal = 4.dp).weight(1F),
                            ) {
                                Text(text = downloadButtonText, overflow = TextOverflow.Ellipsis, maxLines = 1)
                            }
                            Button(
                                onClick = ::onReadButtonClick,
                                colors = ButtonDefaults.buttonColorsPrimary(),
                                modifier = Modifier.padding(horizontal = 4.dp).weight(1F),
                            ) {
                                Text(text = readButtonText, overflow = TextOverflow.Ellipsis, maxLines = 1)
                            }
                        }
                        if (getDetailError.isNotBlank()) {
                            GalleryDetailErrorTip(error = getDetailError, onClick = onRetry)
                        } else if (galleryDetail != null) {
                            BelowHeader(galleryDetail, voteTag)
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(keylineMargin),
                                contentAlignment = Alignment.Center,
                            ) {
                                InfiniteProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
        !windowSizeClass.isExpanded -> BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // 横屏手机/窄平板同样要按可用宽度补足列数，否则固定列数会把预览图拉得过宽
            val stripSpacing = dimensionResource(id = com.hippo.ehviewer.R.dimen.strip_item_padding)
            val gridWidthDp = (maxWidth - keylineMargin * 2).value.roundToInt().coerceAtLeast(1)
            val previewColumns = WindowLayout.thumbGridColumns(gridWidthDp, thumbColumns)
            FastScrollLazyVerticalGrid(
                columns = GridCells.Fixed(previewColumns),
                contentPadding = contentPadding,
                modifier = modifier.padding(horizontal = keylineMargin),
                horizontalArrangement = Arrangement.spacedBy(stripSpacing),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = com.hippo.ehviewer.R.dimen.strip_item_padding_v)),
            ) {
                item(
                    key = "header",
                    span = { GridItemSpan(maxCurrentLineSpan) },
                    contentType = "header",
                ) {
                    GalleryDetailHeaderCard(
                        info = galleryInfo,
                        onInfoCardClick = ::onGalleryInfoCardClick,
                        onUploaderChipClick = ::onUploaderChipClick.partially1(galleryInfo),
                        onBlockUploaderIconClick = ::showFilterUploaderDialog.partially1(galleryInfo),
                        onCategoryChipClick = ::onCategoryChipClick,
                        onCoverClick = ::onCoverClick,
                        modifier = Modifier.fillMaxWidth().padding(vertical = keylineMargin),
                    )
                }
                item(
                    key = "body",
                    span = { GridItemSpan(maxCurrentLineSpan) },
                    contentType = "body",
                ) {
                    LocalPinnableContainer.current!!.run { remember { pin() } }
                    Column {
                        Row {
                            Button(
                                onClick = ::onDownloadButtonClick,
                                colors = ButtonDefaults.buttonColors(),
                                modifier = Modifier.padding(horizontal = 4.dp).weight(1F),
                            ) {
                                Text(text = downloadButtonText, overflow = TextOverflow.Ellipsis, maxLines = 1)
                            }
                            Button(
                                onClick = ::onReadButtonClick,
                                colors = ButtonDefaults.buttonColorsPrimary(),
                                modifier = Modifier.padding(horizontal = 4.dp).weight(1F),
                            ) {
                                Text(text = readButtonText, overflow = TextOverflow.Ellipsis, maxLines = 1)
                            }
                        }
                        if (getDetailError.isNotBlank()) {
                            GalleryDetailErrorTip(error = getDetailError, onClick = onRetry)
                        } else if (galleryDetail != null) {
                            BelowHeader(galleryDetail, voteTag)
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(keylineMargin),
                                contentAlignment = Alignment.Center,
                            ) {
                                InfiniteProgressIndicator()
                            }
                        }
                    }
                }
                if (galleryDetail != null && previews != null) {
                    galleryPreview(galleryDetail, previews) { navToReader(galleryDetail.galleryInfo, it) }
                }
            }
        }
        else -> BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // 列宽必须按网格自身的可用宽度算：用窗口宽度会把列宽算大，宽屏反而退化成单列满宽
            val stripSpacing = dimensionResource(id = com.hippo.ehviewer.R.dimen.strip_item_padding)
            val gridWidthDp = (maxWidth - keylineMargin * 2).value.roundToInt().coerceAtLeast(1)
            // 用固定列数而不是 GridCells.Adaptive：Adaptive 按 (可用宽 + 间距) / (最小列宽 + 间距) 取整，
            // 按可用宽度反推最小列宽时容易少排一列，宽屏下反而比竖屏列数还少。
            val previewColumns = WindowLayout.thumbGridColumns(gridWidthDp, thumbColumns)
            FastScrollLazyVerticalGrid(
                columns = GridCells.Fixed(previewColumns),
                contentPadding = contentPadding,
                modifier = modifier.padding(horizontal = keylineMargin),
                horizontalArrangement = Arrangement.spacedBy(stripSpacing),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = com.hippo.ehviewer.R.dimen.strip_item_padding_v)),
            ) {
                item(
                    key = "header",
                    span = { GridItemSpan(maxCurrentLineSpan) },
                    contentType = "header",
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        GalleryDetailHeaderCard(
                            info = galleryInfo,
                            onInfoCardClick = ::onGalleryInfoCardClick,
                            onUploaderChipClick = ::onUploaderChipClick.partially1(galleryInfo),
                            onBlockUploaderIconClick = ::showFilterUploaderDialog.partially1(galleryInfo),
                            onCategoryChipClick = ::onCategoryChipClick,
                            onCoverClick = ::onCoverClick,
                            modifier = Modifier.width(dimensionResource(id = com.hippo.ehviewer.R.dimen.gallery_detail_card_landscape_width)).padding(vertical = keylineMargin),
                        )
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Spacer(modifier = modifier.height(16.dp))
                            Button(
                                onClick = ::onReadButtonClick,
                                colors = ButtonDefaults.buttonColorsPrimary(),
                                modifier = Modifier.height(56.dp).padding(horizontal = 16.dp).width(192.dp),
                            ) {
                                Text(text = readButtonText, overflow = TextOverflow.Ellipsis, maxLines = 1)
                            }
                            Spacer(modifier = modifier.height(24.dp))
                            Button(
                                onClick = ::onDownloadButtonClick,
                                colors = ButtonDefaults.buttonColors(),
                                modifier = Modifier.height(56.dp).padding(horizontal = 16.dp).width(192.dp),
                            ) {
                                Text(text = downloadButtonText, overflow = TextOverflow.Ellipsis, maxLines = 1)
                            }
                        }
                    }
                }
                item(
                    key = "body",
                    span = { GridItemSpan(maxCurrentLineSpan) },
                    contentType = "body",
                ) {
                    LocalPinnableContainer.current!!.run { remember { pin() } }
                    Column {
                        if (getDetailError.isNotBlank()) {
                            GalleryDetailErrorTip(error = getDetailError, onClick = onRetry)
                        } else if (galleryDetail != null) {
                            BelowHeader(galleryDetail, voteTag)
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(keylineMargin),
                                contentAlignment = Alignment.Center,
                            ) {
                                InfiniteProgressIndicator()
                            }
                        }
                    }
                }
                if (galleryDetail != null && previews != null) {
                    galleryPreview(galleryDetail, previews) { navToReader(galleryDetail.galleryInfo, it) }
                }
            }
        }
    }
}

@Composable
context(ctx: Context, _: CoroutineScope, _: DestinationsNavigator, _: DialogState, _: SnackbarHostState)
fun BelowHeader(galleryDetail: GalleryDetail, voteTag: VoteTag) {
    @Composable
    fun EhIconButton(
        icon: ImageVector,
        text: String,
        onClick: () -> Unit,
    ) = Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTertiaryIconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        }
        Text(text = text)
    }

    @Composable
    fun GalleryDetailComment(commentsList: List<GalleryComment>) {
        val maxShowCount = 2
        val commentText = when {
            commentsList.isEmpty() -> stringResource(R.string.no_comments)
            commentsList.size <= maxShowCount -> stringResource(R.string.no_more_comments)
            else -> stringResource(R.string.more_comment)
        }
        fun navigateToCommentScreen() {
            navigate(GalleryCommentsScreenDestination(galleryDetail.gid, galleryDetail.token))
        }
        CrystalCard {
            commentsList.take(maxShowCount).forEach { item ->
                GalleryCommentCard(
                    modifier = Modifier.padding(vertical = 4.dp),
                    comment = item,
                    onCardClick = ::navigateToCommentScreen,
                    onUserClick = ::navigateToCommentScreen,
                    onUrlClick = {
                        if (it.startsWith("#c")) {
                            navigateToCommentScreen()
                        } else {
                            if (!jumpToReaderByPage(it, galleryDetail)) if (!navWithUrl(it)) openBrowser(it)
                        }
                    },
                    maxLines = 5,
                    ellipsis = true,
                )
            }
            TextButton(
                text = commentText,
                onClick = ::navigateToCommentScreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimensionResource(id = com.hippo.ehviewer.R.dimen.strip_item_padding_v)),
            )
        }
    }
    suspend fun showNewerVersionDialog() {
        val items = galleryDetail.newerVersions.map {
            string(R.string.newer_version_title, it.title, it.posted)
        }
        val selected = awaitSelectItem(items)
        val info = galleryDetail.newerVersions[selected]
        withUIContext {
            // Can't use GalleryInfoArgs as thumbKey is null
            navigate(info.gid asDstWith info.token)
        }
    }
    val keylineMargin = dimensionResource(com.hippo.ehviewer.R.dimen.keyline_margin)
    Spacer(modifier = Modifier.size(keylineMargin))
    if (galleryDetail.newerVersions.isNotEmpty()) {
        Box(contentAlignment = Alignment.Center) {
            CrystalCard(
                onClick = { launchIO { showNewerVersionDialog() } },
                modifier = Modifier.fillMaxWidth().height(32.dp),
            ) {
            }
            Text(text = stringResource(id = R.string.newer_version_available))
        }
        Spacer(modifier = Modifier.size(keylineMargin))
    }
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        val favSlot by FavouriteStatusRouter.collectAsState(galleryDetail) { it }
        val favButtonText = if (favSlot != NOT_FAVORITED) {
            galleryDetail.favoriteName ?: stringResource(id = R.string.local_favorites)
        } else {
            stringResource(id = R.string.not_favorited)
        }
        val favoritesLock = remember { MutatorMutex() }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val removeSucceed = stringResource(R.string.remove_from_favorite_success)
            val addSucceed = stringResource(R.string.add_to_favorite_success)
            val removeFailed = stringResource(R.string.remove_from_favorite_failure)
            val addFailed = stringResource(R.string.add_to_favorite_failure)
            FilledTertiaryIconToggleButton(
                checked = favSlot != NOT_FAVORITED,
                onCheckedChange = {
                    launchIO {
                        favoritesLock.mutate {
                            runSuspendCatching {
                                modifyFavorites(galleryDetail)
                            }.onSuccess { add ->
                                if (add) {
                                    snackbar(addSucceed)
                                } else {
                                    snackbar(removeSucceed)
                                }
                            }.onFailure {
                                // Use the operation that was attempted so removal failures are not reported as additions.
                                snackbar(if (favSlot != NOT_FAVORITED) removeFailed else addFailed)
                            }
                        }
                    }
                },
            ) {
                Icon(
                    imageVector = getFavoriteIcon(favSlot != NOT_FAVORITED),
                    contentDescription = null,
                )
            }
            Text(text = favButtonText)
        }
        EhIconButton(
            icon = MiuixIcons.Search,
            text = stringResource(id = R.string.similar_gallery),
            onClick = {
                val keyword = EhUtils.extractTitle(galleryDetail.title)
                val artistTag = galleryDetail.tagGroups.artistTag()
                if (null != keyword) {
                    navigate(
                        ListUrlBuilder(
                            mode = ListUrlBuilder.MODE_NORMAL,
                            keyword = "\"" + keyword + "\"",
                        ).asDst(),
                    )
                } else if (artistTag != null) {
                    navigate(
                        ListUrlBuilder(
                            mode = ListUrlBuilder.MODE_TAG,
                            keyword = artistTag,
                        ).asDst(),
                    )
                } else if (null != galleryDetail.uploader) {
                    navigate(
                        ListUrlBuilder(
                            mode = ListUrlBuilder.MODE_UPLOADER,
                            keyword = galleryDetail.uploader,
                        ).asDst(),
                    )
                }
            },
        )
        val signInFirst = stringResource(R.string.sign_in_first)
        val noArchive = stringResource(R.string.no_archives)
        val downloadStarted = stringResource(R.string.download_archive_started)
        val downloadFailed = stringResource(R.string.download_archive_failure)
        val archiveResult = remember(galleryDetail) {
            async(Dispatchers.IO + Job(), CoroutineStart.LAZY) {
                with(galleryDetail) {
                    EhEngine.getArchiveList(gid, token)
                }
            }
        }
        fun showArchiveDialog() {
            launchIO {
                if (galleryDetail.apiUid < 0) {
                    snackbar(signInFirst)
                } else {
                    runSuspendCatching {
                        val (archiveList, funds) = bgWork { archiveResult.await() }
                        if (archiveList.isEmpty()) {
                            snackbar(noArchive)
                        } else {
                            val selected = showNoButton {
                                ArchiveList(
                                    funds = funds,
                                    items = archiveList,
                                    onItemClick = { resume(it) },
                                )
                            }
                            EhUtils.downloadArchive(galleryDetail, selected)
                            snackbar(downloadStarted)
                        }
                    }.onFailure {
                        when (it) {
                            is NoHathClientException -> snackbar(it.message!!)
                            is EhException -> snackbar(it.displayString())
                            else -> {
                                logcat(it)
                                snackbar(downloadFailed)
                            }
                        }
                    }
                }
            }
        }
        EhIconButton(
            icon = MiuixIcons.Folder,
            text = stringResource(id = R.string.archive),
            onClick = ::showArchiveDialog,
        )
        val torrentText = stringResource(R.string.torrent_count, galleryDetail.torrentCount)
        val noTorrents = stringResource(R.string.no_torrents)
        val torrentResult = remember(galleryDetail) {
            async(Dispatchers.IO + Job(), CoroutineStart.LAZY) {
                parZip(
                    { EhEngine.getTorrentList(galleryDetail.gid, galleryDetail.token) },
                    { EhEngine.getTorrentKey() },
                    { list, key -> list to key },
                )
            }
        }
        suspend fun showTorrentDialog() {
            val (torrentList, key) = bgWork { torrentResult.await() }
            if (torrentList.isEmpty()) {
                snackbar(noTorrents)
            } else {
                val selected = showNoButton(false) {
                    TorrentList(
                        items = torrentList,
                        onItemClick = { resume(it) },
                    )
                }
                val hash = selected.url.dropLast(8).takeLast(40)
                val name = selected.name.encodeURLParameter()
                val tracker = EhUrl.getTrackerUrl(galleryDetail.gid, key).encodeURLParameter()
                val link = "magnet:?xt=urn:btih:$hash&dn=$name&tr=$tracker"
                val intent = Intent(Intent.ACTION_VIEW, link.toUri())
                try {
                    ctx.startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    withUIContext { addTextToClipboard(link, true) }
                }
            }
        }
        EhIconButton(
            icon = EhIcons.Default.Magnet,
            text = torrentText,
            onClick = {
                launchIO {
                    when {
                        galleryDetail.torrentCount <= 0 -> snackbar(noTorrents)
                        else -> runSwallowingWithUI { showTorrentDialog() }
                    }
                }
            },
        )
    }
    Spacer(modifier = Modifier.size(keylineMargin))
    fun getAllRatingText(rating: Float, ratingCount: Int): String = string(
        R.string.rating_text,
        string(getRatingText(rating)),
        rating,
        ratingCount,
    )
    var ratingText by rememberSaveable {
        mutableStateOf(getAllRatingText(galleryDetail.rating, galleryDetail.ratingCount))
    }
    val rateSucceed = stringResource(R.string.rate_successfully)
    val rateFailed = stringResource(R.string.rate_failed)
    val signInFirst = stringResource(R.string.sign_in_first)
    fun showRateDialog() {
        launchIO {
            if (galleryDetail.apiUid < 0) {
                snackbar(signInFirst)
                return@launchIO
            }
            val pendingRating = awaitResult(galleryDetail.rating.coerceAtLeast(.5f), title = R.string.rate) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    var text by remember { mutableIntStateOf(getRatingText(expectedValue)) }
                    Text(text = stringResource(id = text), style = MiuixTheme.textStyles.body1)
                    Spacer(modifier = Modifier.size(keylineMargin))
                    GalleryRatingBar(
                        rating = expectedValue,
                        onRatingChange = {
                            expectedValue = it.coerceAtLeast(.5f)
                            text = getRatingText(expectedValue)
                        },
                    )
                }
            }
            galleryDetail.runSuspendCatching {
                EhEngine.rateGallery(apiUid, apiKey, gid, token, pendingRating)
            }.onSuccess { result ->
                galleryDetail.apply {
                    rating = result.rating
                    ratingCount = result.ratingCount
                }
                ratingText = getAllRatingText(result.rating, result.ratingCount)
                snackbar(rateSucceed)
            }.onFailure {
                logcat(it)
                snackbar(rateFailed)
            }
        }
    }
    CrystalCard(onClick = ::showRateDialog) {
        Column(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GalleryDetailRating(rating = galleryDetail.rating)
            Spacer(modifier = Modifier.size(keylineMargin))
            Text(text = ratingText)
        }
    }
    Spacer(modifier = Modifier.size(keylineMargin))
    val tags = galleryDetail.tagGroups
    if (tags.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(id = R.string.no_tags))
        }
    } else {
        val copy = stringResource(android.R.string.copy)
        val copyTrans = stringResource(R.string.copy_trans)
        val showDefine = stringResource(R.string.show_definition)
        val addFilter = stringResource(R.string.add_filter)
        val filterAdded = stringResource(R.string.filter_added)
        val upTag = stringResource(R.string.tag_vote_up)
        val downTag = stringResource(R.string.tag_vote_down)
        val withDraw = stringResource(R.string.tag_vote_withdraw)
        fun search(tag: String) = navigate(ListUrlBuilder(mode = ListUrlBuilder.MODE_TAG, keyword = tag).asDst())
        GalleryTags(
            tagGroups = tags,
            onTagClick = ::search,
            onTagLongClick = { tag, translation, vote ->
                val rawValue = tag.substringAfter(':')
                launchIO {
                    awaitSelectAction {
                        onSelect(ctx.getString(R.string.search_bar_hint, tag)) {
                            withUIContext { search(tag) }
                        }
                        onSelect(copy) {
                            addTextToClipboard(tag)
                        }
                        if (rawValue != translation) {
                            onSelect(copyTrans) {
                                addTextToClipboard(translation)
                            }
                        }
                        onSelect(showDefine) {
                            openBrowser(EhUrl.getTagDefinitionUrl(rawValue))
                        }
                        onSelect(addFilter) {
                            awaitConfirmationOrCancel { Text(text = stringResource(R.string.filter_the_tag, tag)) }
                            Filter(FilterMode.TAG, tag).remember()
                            snackbar(filterAdded)
                        }
                        if (galleryDetail.apiUid >= 0) {
                            when (vote) {
                                VoteStatus.None -> {
                                    onSelect(upTag) { galleryDetail.voteTag(tag, 1) }
                                    onSelect(downTag) { galleryDetail.voteTag(tag, -1) }
                                }
                                VoteStatus.Up -> onSelect(withDraw) { galleryDetail.voteTag(tag, -1) }
                                VoteStatus.Down -> onSelect(withDraw) { galleryDetail.voteTag(tag, 1) }
                            }
                        }
                    }()
                }
            },
        )
    }
    Spacer(modifier = Modifier.size(keylineMargin))
    if (Settings.showComments.value) {
        GalleryDetailComment(galleryDetail.comments.comments)
        Spacer(modifier = Modifier.size(dimensionResource(id = com.hippo.ehviewer.R.dimen.strip_item_padding_v)))
    }
}

@StringRes
private fun getRatingText(rating: Float): Int = when ((rating * 2).roundToInt()) {
    1 -> R.string.rating1
    2 -> R.string.rating2
    3 -> R.string.rating3
    4 -> R.string.rating4
    5 -> R.string.rating5
    6 -> R.string.rating6
    7 -> R.string.rating7
    8 -> R.string.rating8
    9 -> R.string.rating9
    10 -> R.string.rating10
    else -> R.string.rating_none
}

private fun List<GalleryTagGroup>.artistTag() = find { (ns, _) -> ns == TagNamespace.Artist || ns == TagNamespace.Cosplayer }?.let { (ns, tags) -> "${ns.value}:${tags[0].text}" }

@Composable
context(_: Context)
private fun GalleryDetail.collectPreviewItems() = rememberInVM(previewList) {
    val pageSize = previewList.size
    val pages = pages
    val previewPagesMap = previewList.associateBy { it.position } as MutableMap
    Pager(
        PagingConfig(
            pageSize = pageSize,
            prefetchDistance = pageSize.coerceAtMost(100),
            initialLoadSize = pageSize,
            jumpThreshold = 2 * pageSize,
        ),
    ) {
        object : PagingSource<Int, GalleryPreview>() {
            override fun getRefreshKey(state: PagingState<Int, GalleryPreview>) = state.getClippedRefreshKey()
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GalleryPreview> = withIOContext {
                val key = params.key ?: 0
                val up = getOffset(params, key, pages)
                val end = (up + getLimit(params, key) - 1).coerceAtMost(pages - 1)
                runSuspendCatching {
                    (up..end).filterNot { it in previewPagesMap }.map { it / pageSize }.toSet()
                        .parMap(concurrency = Settings.multiThreadDownload.value) { page ->
                            val url = EhUrl.getGalleryDetailUrl(gid, token, page)
                            EhEngine.getPreviewList(url).previews
                        }.flattenForEach {
                            previewPagesMap[it.position] = it
                            if (Settings.preloadThumbAggressively.value) {
                                imageRequest(it) { justDownload() }.executeIn(viewModelScope)
                            }
                        }
                }.foldToLoadResult {
                    val r = (up..end).map { requireNotNull(previewPagesMap[it]) }
                    val prevK = if (up <= 0 || r.isEmpty()) null else up
                    val nextK = if (end == pages - 1) null else end + 1
                    LoadResult.Page(r, prevK, nextK, up, pages - end - 1)
                }
            }
            override val jumpingSupported = true
        }
    }.flow.cachedIn(viewModelScope)
}.collectAsLazyPagingItems()

context(_: Context)
private fun LazyGridScope.galleryPreview(detail: GalleryDetail, data: LazyPagingItems<GalleryPreview>, onClick: (Int) -> Unit) {
    val isV2Thumb = detail.previewList.first() is V2GalleryPreview
    items(
        count = data.itemCount,
        key = data.itemKey(key = { item -> item.position }),
        contentType = { "preview" },
    ) { index ->
        val item = data[index]
        EhPreviewItem(item, index) { onClick(index) }
        PrefetchAround(data, index, if (isV2Thumb) 20 else 6) { imageRequest(it) }
    }
}
