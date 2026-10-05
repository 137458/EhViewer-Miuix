package com.ehviewer.core.shell.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ehviewer.core.i18n.MR
import com.ehviewer.core.ui.component.LiquidGlassSurface
import com.ehviewer.core.ui.component.LocalBackdrop
import com.ehviewer.core.ui.component.SquircleShape
import com.ehviewer.core.ui.component.blurBackdropSource
import com.ehviewer.core.ui.component.rememberBlurBackdrop
import com.ehviewer.core.ui.icons.EhIcons
import com.ehviewer.core.ui.icons.filled.MenuBook
import com.ehviewer.core.ui.util.LocalBottomBarContentPadding
import com.ehviewer.core.ui.util.thenIf
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.SearchBarDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

// 搜索建议条目（宿主供给；桌面=本地搜索历史，移动端=搜索库+标签翻译，共享层不感知）
data class SearchSuggestion(
    val keyword: String,
    val hint: String? = null,
    val canDelete: Boolean = false,
    val canOpenDirectly: Boolean = false,
)

private val SearchBarHorizontalPadding = 16.dp
private val M3SearchBarMaxWidth = 720.dp

// 库页搜索顶栏（移动端 SearchBarScreen 同构移植）：
// 折叠态=液态玻璃搜索胶囊（标题占位）+ 紧凑条标题；展开态=全屏搜索与建议列表；
// 筛选经底单弹层承载。建议数据与删除确认由宿主回调注入，滚动折叠偏移由宿主提供。
@Composable
fun LibrarySearchBarScaffold(
    onApplySearch: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    title: String?,
    searchFieldHint: String,
    searchFieldState: TextFieldState = rememberTextFieldState(),
    suggestionsProvider: suspend (String) -> List<SearchSuggestion> = { emptyList() },
    searchBarOffsetY: () -> Int = { 0 },
    trailingIcon: @Composable () -> Unit = {},
    subHeader: @Composable (() -> Unit)? = null,
    filter: @Composable (() -> Unit)? = null,
    floatingActionButton: @Composable () -> Unit = {},
    onDeleteSuggestion: suspend (String) -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    var suggestionList by remember { mutableStateOf(emptyList<SearchSuggestion>()) }
    val suggestionScope = androidx.compose.runtime.rememberCoroutineScope()

    if (expanded) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            androidx.compose.runtime.snapshotFlow { searchFieldState.text }.collectLatest {
                suggestionList = suggestionsProvider(it.toString())
            }
        }
    }

    fun hideSearchView() {
        onExpandedChange(false)
    }

    fun onApplySearchInternal() {
        // 剪贴板粘贴可能带非法空白，归一为单空格
        val query = searchFieldState.text.trim().replace(WhitespaceRegex, " ")
        onApplySearch(query)
    }

    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    val backdrop = rememberBlurBackdrop()

    CompositionLocalProvider(LocalBackdrop provides backdrop) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val subHeaderHeight = if (subHeader != null) 40.dp else 0.dp
            val compactBarHeight = 48.dp
            val fullBarHeight = SearchBarDefaults.InputFieldMinHeight + 16.dp + subHeaderHeight
            // 折叠阈值必须与顶栏完整高度同口径，否则带分类条时折叠进度永远到不了终态
            val collapseThresholdPx = with(density) { fullBarHeight.toPx() }
            val collapseProgress = if (expanded) 0f else (-searchBarOffsetY().toFloat() / collapseThresholdPx).coerceIn(0f, 1f)

            Scaffold(
                topBar = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val dynamicBlurHeight = statusBarPadding + fullBarHeight + (compactBarHeight - fullBarHeight) * collapseProgress
                        if (backdrop != null && isRuntimeShaderSupported()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(dynamicBlurHeight)
                                    .progressiveTextureBlur(
                                        backdrop = backdrop,
                                        shape = RectangleShape,
                                        gradient = ProgressiveBlur.Top.copy(curve = 2.2f),
                                        blurRadius = 10f,
                                        colors = BlurDefaults.blurColors(
                                            blendColors = listOf(
                                                BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(alpha = 0.35f)),
                                            ),
                                        ),
                                    ),
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(dynamicBlurHeight)
                                    .background(MiuixTheme.colorScheme.background.copy(alpha = 0.85f)),
                            )
                        }
                        Column {
                            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                            // 占位，撑出 SearchBar 的固有 padding
                            Spacer(modifier = Modifier.height(fullBarHeight))
                        }
                    }
                },
                floatingActionButton = floatingActionButton,
                content = { paddingValues ->
                    val bottomBarPadding = LocalBottomBarContentPadding.current
                    val layoutDirection = LocalLayoutDirection.current
                    val effectivePaddingValues = remember(paddingValues, bottomBarPadding, layoutDirection) {
                        PaddingValues(
                            start = paddingValues.calculateStartPadding(layoutDirection),
                            top = paddingValues.calculateTopPadding(),
                            end = paddingValues.calculateEndPadding(layoutDirection),
                            bottom = maxOf(paddingValues.calculateBottomPadding(), bottomBarPadding),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .blurBackdropSource(backdrop),
                    ) {
                        content(effectivePaddingValues)
                    }
                },
            )
            // 非触屏模式下 SearchBar 展开态无法靠重聚焦退出：1dp 可聚焦锚点兜底
            Box(Modifier.size(1.dp).focusable())
            val query = searchFieldState.text.toString()
            val placeholder = title.takeUnless { expanded } ?: searchFieldHint
            val searchBg = MiuixTheme.colorScheme.background
            if (expanded) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(searchBg),
                )
            }
            CompositionLocalProvider(LocalBackdrop provides null) {
                SearchBar(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal))
                        .thenIf(!expanded) {
                            offset { androidx.compose.ui.unit.IntOffset(0, searchBarOffsetY()) }
                                .graphicsLayer {
                                    alpha = (1f - collapseProgress * 1.5f).coerceIn(0f, 1f)
                                    if (collapseProgress >= 0.8f) {
                                        translationY = -10000f
                                    }
                                }
                        }
                        .thenIf(expanded) { fillMaxSize() },
                    inputField = {
                        val inputFieldModifier = Modifier
                            .widthIn(max = (maxWidth - SearchBarHorizontalPadding * 2).coerceAtMost(M3SearchBarMaxWidth))
                            .fillMaxWidth()

                        val inputContent = @Composable {
                            InputField(
                                query = query,
                                onQueryChange = { searchFieldState.setTextAndPlaceCursorAtEnd(it) },
                                onSearch = {
                                    hideSearchView()
                                    onApplySearchInternal()
                                },
                                expanded = expanded,
                                onExpandedChange = onExpandedChange,
                                modifier = if (!expanded) Modifier.fillMaxWidth() else inputFieldModifier,
                                label = placeholder,
                                color = if (expanded) MiuixTheme.colorScheme.surfaceContainer else Color.Transparent,
                                leadingIcon = {
                                    if (expanded) {
                                        IconButton(onClick = { hideSearchView() }) {
                                            Icon(MiuixIcons.Back, contentDescription = null)
                                        }
                                    } else {
                                        IconButton(onClick = { onExpandedChange(true) }) {
                                            Icon(MiuixIcons.Search, contentDescription = null)
                                        }
                                    }
                                },
                                trailingIcon = {
                                    if (expanded) {
                                        AnimatedContent(targetState = query.isNotEmpty()) { hasText ->
                                            if (hasText) {
                                                IconButton(onClick = { searchFieldState.clearText() }) {
                                                    Icon(MiuixIcons.Close, contentDescription = null)
                                                }
                                            } else {
                                                Spacer(Modifier)
                                            }
                                        }
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (filter != null) {
                                                IconButton(onClick = { showFilterSheet = true }) {
                                                    Icon(MiuixIcons.More, contentDescription = stringResource(MR.strings.desktop_filter))
                                                }
                                            }
                                            trailingIcon()
                                        }
                                    }
                                },
                            )
                        }

                        if (!expanded) {
                            LiquidGlassSurface(
                                shape = CircleShape,
                                elevation = 4.dp,
                                refractionHeight = 16.dp,
                                refractionAmount = 16.dp,
                                containerColor = MiuixTheme.colorScheme.surfaceContainerHigh,
                                modifier = inputFieldModifier,
                                backdrop = backdrop,
                            ) {
                                inputContent()
                            }
                        } else {
                            inputContent()
                        }
                    },
                    expanded = expanded,
                    onExpandedChange = onExpandedChange,
                    outsideEndAction = {
                        TextButton(
                            text = stringResource(MR.strings.desktop_action_cancel),
                            onClick = { hideSearchView() },
                            modifier = Modifier.padding(end = 12.dp),
                        )
                    },
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues(),
                    ) {
                        if (filter != null) {
                            item(key = "search_filter_header") {
                                filter()
                            }
                        }
                        items(suggestionList, key = { it.keyword.hashCode() * 31 + it.canDelete.hashCode() }) {
                            SuggestionRow(
                                suggestion = it,
                                onApply = { keyword ->
                                    searchFieldState.setTextAndPlaceCursorAtEnd(keyword)
                                },
                                onDelete = { keyword ->
                                    // 删除确认在宿主回调内（桌面确认卡 / 移动端 DialogState）
                                    suggestionScope.launch {
                                        onDeleteSuggestion(keyword)
                                        suggestionList = suggestionsProvider(searchFieldState.text.toString())
                                    }
                                },
                            )
                        }
                    }
                }
            }

            if (!expanded && subHeader != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal))
                        .offset {
                            val inputFieldOffset = with(density) { (SearchBarDefaults.InputFieldMinHeight + 16.dp).roundToPx() }
                            androidx.compose.ui.unit.IntOffset(0, searchBarOffsetY() + inputFieldOffset)
                        }
                        .graphicsLayer {
                            alpha = (1f - collapseProgress * 1.5f).coerceIn(0f, 1f)
                            if (collapseProgress >= 0.8f) {
                                translationY = -10000f
                            }
                        }
                        .fillMaxWidth(),
                ) {
                    subHeader()
                }
            }

            if (!expanded) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal))
                        .fillMaxWidth()
                        .height(compactBarHeight),
                ) {
                    if (title != null) {
                        Text(
                            text = title,
                            style = MiuixTheme.textStyles.title2,
                            color = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 24.dp)
                                .graphicsLayer {
                                    alpha = ((collapseProgress - 0.3f) / 0.7f).coerceIn(0f, 1f)
                                },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp)
                            .graphicsLayer {
                                alpha = ((collapseProgress - 0.2f) / 0.8f).coerceIn(0f, 1f)
                                scaleX = 0.85f + 0.15f * collapseProgress
                                scaleY = 0.85f + 0.15f * collapseProgress
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (collapseProgress > 0.2f) {
                            IconButton(
                                onClick = { onExpandedChange(true) },
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Search,
                                    contentDescription = null,
                                )
                            }
                            if (filter != null) {
                                IconButton(
                                    onClick = { showFilterSheet = true },
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.More,
                                        contentDescription = stringResource(MR.strings.desktop_filter),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (filter != null) {
                WindowBottomSheet(
                    show = showFilterSheet,
                    onDismissRequest = { showFilterSheet = false },
                ) {
                    CompositionLocalProvider(LocalBackdrop provides null) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                                // 滚动兜底只能加在弹层这一侧：同一 filter() 也作为 LazyColumn item 渲染，
                                // item 内嵌 verticalScroll 会因无限高度约束直接抛异常
                                .verticalScroll(rememberScrollState()),
                        ) {
                            filter()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    suggestion: SearchSuggestion,
    onApply: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(SquircleShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(SquircleShape(12.dp))
                .clickable(role = Role.Button) { onApply(suggestion.keyword) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (suggestion.canOpenDirectly) {
                Icon(
                    imageVector = EhIcons.Default.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 12.dp).size(20.dp),
                    tint = MiuixTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = suggestion.keyword,
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.body1,
                )
                if (suggestion.hint != null) {
                    Text(
                        text = suggestion.hint,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.body2,
                    )
                }
            }
        }
        if (suggestion.canDelete) {
            IconButton(
                onClick = { onDelete(suggestion.keyword) },
                modifier = Modifier.padding(end = 8.dp),
            ) {
                Icon(
                    imageVector = MiuixIcons.Close,
                    contentDescription = stringResource(MR.strings.delete),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private val WhitespaceRegex = Regex("\\s+")
