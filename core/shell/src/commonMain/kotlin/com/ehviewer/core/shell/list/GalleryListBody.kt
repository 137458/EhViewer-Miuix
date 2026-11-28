package com.ehviewer.core.shell.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ehviewer.core.i18n.MR
import com.ehviewer.core.model.BaseGalleryInfo
import com.ehviewer.core.ui.component.ErrorTip
import com.ehviewer.core.ui.component.FastScrollLazyVerticalGrid
import com.ehviewer.core.ui.component.FastScrollLazyVerticalStaggeredGrid
import com.ehviewer.core.ui.util.WindowLayout
import dev.icerock.moko.resources.compose.stringResource
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState

// 与 :app dimens 同值的布局常量（shared 无法读 app 资源，对应 gallery_list_margin_h 等）
private val GALLERY_LIST_MARGIN_H = 12.dp
private val GALLERY_LIST_MARGIN_V = 4.dp
private val GALLERY_LIST_INTERVAL = 12.dp
private val GALLERY_GRID_INTERVAL = 4.dp

// 详情卡列表的默认最小列宽（移动端 short 档；宿主有列宽设置时传入覆盖）
const val GALLERY_DETAIL_COLUMN_WIDTH_DEFAULT_DP = 320

// 触底提前量：可见项距列表末尾不足该数量条目时触发 onLoadMore
private const val LOAD_MORE_THRESHOLD = 4

// 库页列表体（移动端 GalleryList 同构移植，数据面收纯 List + 分页回调，由宿主供数）：
// listMode==0 详情卡网格 / 其他为瀑布流缩略图；下拉刷新、触底加载、加载态覆盖层与移动端一致。
// refreshing 的复位由宿主在刷新完成后自行置回 false。
@Composable
fun GalleryListBody(
    items: List<BaseGalleryInfo>,
    listMode: Int,
    emptyText: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    detailColumnWidthDp: Int = GALLERY_DETAIL_COLUMN_WIDTH_DEFAULT_DP,
    thumbColumnsConfigured: Int = 0,
    refreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    loadingMore: Boolean = false,
    loadMoreError: String? = null,
    onRetryLoadMore: () -> Unit = {},
    detailListState: LazyGridState = rememberLazyGridState(),
    thumbListState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    detailItemContent: @Composable LazyGridItemScope.(BaseGalleryInfo) -> Unit,
    thumbItemContent: @Composable LazyStaggeredGridItemScope.(BaseGalleryInfo) -> Unit,
) {
    val refreshState = rememberPullToRefreshState()
    PullToRefresh(
        isRefreshing = refreshing,
        onRefresh = {
            // Miuix PullToRefresh 无 enabled 参数：刷新中忽略手势，不排队重复刷新
            if (!refreshing) onRefresh()
        },
        pullToRefreshState = refreshState,
        modifier = modifier,
        contentPadding = contentPadding,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // 列宽按网格自身可用宽度算：contentPadding 叠加的左右外边距不计入可排布宽度（移动端同注释）
            val availableWidthDp = (maxWidth - GALLERY_LIST_MARGIN_H * 2).value.roundToInt().coerceAtLeast(1)
            val showLoadMoreIndicator = loadingMore || loadMoreError != null

            if (listMode == 0) {
                val listSpacing = GALLERY_LIST_INTERVAL
                // 宽屏下压最小列宽，保证详情列表至少两列而不是被拉伸成单列满宽
                val columnWidth = WindowLayout.detailMinColumnWidth(
                    availableWidthDp = availableWidthDp,
                    configuredMinWidthDp = detailColumnWidthDp,
                    spacingDp = listSpacing.value.roundToInt(),
                ).dp
                LoadMoreEffect(detailListState, items.size, onLoadMore)
                FastScrollLazyVerticalGrid(
                    columns = GridCells.Adaptive(columnWidth),
                    modifier = Modifier.fillMaxSize(),
                    state = detailListState,
                    contentPadding = contentPadding + PaddingValues(GALLERY_LIST_MARGIN_H, GALLERY_LIST_MARGIN_V),
                    verticalArrangement = Arrangement.spacedBy(listSpacing),
                    horizontalArrangement = Arrangement.spacedBy(listSpacing),
                ) {
                    galleryItems(items, detailItemContent)
                    if (showLoadMoreIndicator) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            LoadMoreIndicator(
                                loading = loadingMore,
                                error = loadMoreError,
                                onRetry = onRetryLoadMore,
                            )
                        }
                    }
                }
            } else {
                val thumbColumns = WindowLayout.thumbGridColumns(availableWidthDp, thumbColumnsConfigured)
                LoadMoreEffectStaggered(thumbListState, items.size, onLoadMore)
                FastScrollLazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(thumbColumns),
                    modifier = Modifier.fillMaxSize(),
                    state = thumbListState,
                    contentPadding = contentPadding + PaddingValues(GALLERY_LIST_MARGIN_H, GALLERY_LIST_MARGIN_V),
                    verticalItemSpacing = GALLERY_GRID_INTERVAL,
                    horizontalArrangement = Arrangement.spacedBy(GALLERY_GRID_INTERVAL),
                ) {
                    galleryItemsStaggered(items, thumbItemContent)
                    if (showLoadMoreIndicator) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            LoadMoreIndicator(
                                loading = loadingMore,
                                error = loadMoreError,
                                onRetry = onRetryLoadMore,
                            )
                        }
                    }
                }
            }

            if (refreshing) {
                Surface {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        InfiniteProgressIndicator()
                    }
                }
            }
            if (items.isEmpty() && !refreshing) {
                ErrorTip(modifier = Modifier.widthIn(max = 228.dp), text = emptyText)
            }
        }
    }
}

// gid 作 key 的条目渲染
private fun LazyGridScope.galleryItems(
    items: List<BaseGalleryInfo>,
    itemContent: @Composable LazyGridItemScope.(BaseGalleryInfo) -> Unit,
) {
    items(
        count = items.size,
        key = { index -> items[index].gid },
    ) { index ->
        itemContent(items[index])
    }
}

private fun LazyStaggeredGridScope.galleryItemsStaggered(
    items: List<BaseGalleryInfo>,
    itemContent: @Composable LazyStaggeredGridItemScope.(BaseGalleryInfo) -> Unit,
) {
    items(
        count = items.size,
        key = { index -> items[index].gid },
    ) { index ->
        itemContent(items[index])
    }
}

// 触底加载检测：网格级一个监听（原先每个条目各起一份 derivedStateOf + LaunchedEffect）
@Composable
private fun LoadMoreEffect(
    state: LazyGridState,
    count: Int,
    onLoadMore: () -> Unit,
) {
    val loadMore by rememberUpdatedState(onLoadMore)
    LaunchedEffect(state, count) {
        if (count <= 0) return@LaunchedEffect
        snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .first { lastVisible -> lastVisible >= count - LOAD_MORE_THRESHOLD }
        loadMore()
    }
}

@Composable
private fun LoadMoreEffectStaggered(
    state: LazyStaggeredGridState,
    count: Int,
    onLoadMore: () -> Unit,
) {
    val loadMore by rememberUpdatedState(onLoadMore)
    LaunchedEffect(state, count) {
        if (count <= 0) return@LaunchedEffect
        snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .first { lastVisible -> lastVisible >= count - LOAD_MORE_THRESHOLD }
        loadMore()
    }
}

// 加载更多指示器（移动端 LoadStateIndicator 的 List 化变体）
@Composable
fun LoadMoreIndicator(
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        loading -> LinearProgressIndicator(modifier = modifier)
        error != null -> ErrorTip(
            modifier = modifier,
            text = error,
            retryText = stringResource(MR.strings.action_retry),
            onRetry = onRetry,
            compact = true,
        )
    }
}
