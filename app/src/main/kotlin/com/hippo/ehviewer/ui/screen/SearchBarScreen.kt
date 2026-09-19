package com.hippo.ehviewer.ui.screen

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.displayCutout
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import com.ehviewer.core.database.dao.SearchDao
import com.ehviewer.core.database.model.Search
import com.ehviewer.core.i18n.R
import com.ehviewer.core.model.TagNamespace
import com.ehviewer.core.ui.component.LiquidGlassSurface
import com.ehviewer.core.ui.component.LocalBackdrop
import com.ehviewer.core.ui.component.SquircleShape
import com.ehviewer.core.ui.component.blurBackdropSource
import com.ehviewer.core.ui.component.rememberBlurBackdrop
import com.ehviewer.core.ui.icons.EhIcons
import com.ehviewer.core.ui.icons.filled.MenuBook
import com.ehviewer.core.ui.util.LocalBottomBarContentPadding
import com.ehviewer.core.ui.util.ifNotNullThen
import com.ehviewer.core.ui.util.ifTrueThen
import com.ehviewer.core.ui.util.thenIf
import com.hippo.ehviewer.EhApplication.Companion.searchDatabase
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.ui.destinations.ImageSearchScreenDestination
import com.hippo.ehviewer.ui.theme.scrim
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.ui.tools.awaitConfirmationOrCancel
import com.hippo.ehviewer.ui.tools.rememberCompositionActiveState
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import moe.tarsin.navigate
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
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

fun interface SuggestionProvider {
    suspend fun providerSuggestions(text: String): List<Suggestion>
}

abstract class Suggestion {
    abstract val keyword: String
    open val hint: String? = null
    abstract fun onClick()
    open val canDelete: Boolean = false
    open val canOpenDirectly: Boolean = false
}

suspend fun SearchDao.suggestions(prefix: String, limit: Int) = (if (prefix.isBlank()) list(limit) else rawSuggestions(prefix, limit))

@Composable
context(_: DialogState, _: DestinationsNavigator)
fun SearchBarScreen(
    onApplySearch: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    title: String?,
    searchFieldHint: String,
    searchFieldState: TextFieldState = rememberTextFieldState(),
    suggestionProvider: SuggestionProvider? = null,
    localSearch: Boolean = true,
    searchBarOffsetY: () -> Int = { 0 },
    trailingIcon: @Composable () -> Unit = {},
    subHeader: @Composable (() -> Unit)? = null,
    filter: @Composable (() -> Unit)? = null,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    var mSuggestionList by remember { mutableStateOf(emptyList<Suggestion>()) }
    val mSearchDatabase = searchDatabase.searchDao()
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val context = LocalContext.current
    val animateItems by Settings.animateItems.collectAsState()

    class TagSuggestion(
        override val hint: String?,
        override val keyword: String,
    ) : Suggestion() {
        override fun onClick() {
            val query = searchFieldState.text.toString()
            val (index, keyword) = if (localSearch) {
                query.lastIndexOf(' ') to
                    "${keyword.substringAfter(':')} "
            } else {
                query.lastIndexOfAny(TagTerminators) to
                    if (keyword.endsWith(':')) keyword else "${wrapTagKeyword(keyword)} "
            }
            val keywords = if (index == -1) {
                keyword
            } else {
                "${query.substring(0, index + 1).trimEnd()} $keyword"
            }
            searchFieldState.setTextAndPlaceCursorAtEnd(keywords)
        }
    }

    class KeywordSuggestion(
        override val keyword: String,
    ) : Suggestion() {
        override val canDelete = true
        override fun onClick() {
            searchFieldState.setTextAndPlaceCursorAtEnd(keyword)
        }
    }

    fun mergedSuggestionFlow(): Flow<Suggestion> = with(context) {
        flow {
            val query = searchFieldState.text.toString()
            suggestionProvider?.run { providerSuggestions(query).forEach { emit(it) } }
            mSearchDatabase.suggestions(query, 128).forEach { emit(KeywordSuggestion(it)) }
            val index = if (localSearch) query.lastIndexOf(' ') else query.lastIndexOfAny(TagTerminators)
            val keyword = query.substring(index + 1).trimStart()
            if (keyword.isNotEmpty()) {
                EhTagDatabase.suggestion(keyword, Settings.showTagTranslations.value).take(50)
                    .forEach { emit(TagSuggestion(it.hint, it.tag)) }
            }
        }
    }

    suspend fun updateSuggestions() {
        mSuggestionList = mergedSuggestionFlow().toList()
    }

    if (expanded) {
        LaunchedEffect(Unit) {
            snapshotFlow { searchFieldState.text }.collectLatest {
                updateSuggestions()
            }
        }
    }

    fun hideSearchView() {
        onExpandedChange(false)
    }

    fun onApplySearch() {
        // May have invalid whitespaces if pasted from clipboard, replace them with spaces
        val query = searchFieldState.text.trim().replace(WhitespaceRegex, " ")
        if (query.isNotEmpty()) {
            scope.launch {
                mSearchDatabase.deleteQuery(query)
                val search = Search(System.currentTimeMillis(), query)
                mSearchDatabase.insert(search)
            }
        }
        onApplySearch(query)
    }

    fun deleteKeyword(keyword: String) {
        scope.launch {
            awaitConfirmationOrCancel(confirmText = R.string.delete) {
                Text(text = stringResource(id = R.string.delete_search_history, keyword))
            }
            mSearchDatabase.deleteQuery(keyword)
            updateSuggestions()
        }
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
                            val scrim = MiuixTheme.colorScheme.background.scrim()
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(dynamicBlurHeight)
                                    .background(scrim),
                            )
                        }
                        Column {
                            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                            // Placeholder, fill immutable SearchBar padding
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
            // https://issuetracker.google.com/337191298
            // Workaround for can't exit SearchBar due to refocus in non-touch mode
            Box(Modifier.size(1.dp).focusable())
            val activeState = rememberCompositionActiveState()
            val query = searchFieldState.text.toString()
            val contentActive by activeState.state
            val placeholder = title.takeUnless { expanded || contentActive } ?: searchFieldHint
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
                            offset { IntOffset(0, searchBarOffsetY()) }
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
                                    onApplySearch()
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
                                                IconButton(onClick = { navigate(ImageSearchScreenDestination) }) {
                                                    Icon(MiuixIcons.Image, contentDescription = null)
                                                }
                                            }
                                        }
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (filter != null) {
                                                IconButton(onClick = { showFilterSheet = true }) {
                                                    Icon(MiuixIcons.More, contentDescription = stringResource(R.string.more_actions))
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
                            text = stringResource(id = android.R.string.cancel),
                            onClick = { hideSearchView() },
                            modifier = Modifier.padding(end = 12.dp),
                        )
                    },
                ) {
                    activeState.Anchor()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues(),
                    ) {
                        if (filter != null) {
                            item(key = "search_filter_header") {
                                filter()
                            }
                        }
                        items(mSuggestionList, key = { it.keyword.hashCode() * 31 + it.canDelete.hashCode() }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 2.dp)
                                    .clip(SquircleShape(12.dp))
                                    .thenIf(animateItems) { animateItem() },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(SquircleShape(12.dp))
                                        .clickable(role = Role.Button) { it.onClick() }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (it.canOpenDirectly) {
                                        Icon(
                                            imageVector = EhIcons.Default.MenuBook,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 12.dp).size(20.dp),
                                            tint = MiuixTheme.colorScheme.primary,
                                        )
                                    }
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = it.keyword,
                                            color = MiuixTheme.colorScheme.onSurface,
                                            style = MiuixTheme.textStyles.body1,
                                        )
                                        if (it.hint != null) {
                                            Text(
                                                text = it.hint!!,
                                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                                style = MiuixTheme.textStyles.body2,
                                            )
                                        }
                                    }
                                }
                                if (it.canDelete) {
                                    IconButton(
                                        onClick = { deleteKeyword(it.keyword) },
                                        modifier = Modifier.padding(end = 8.dp),
                                    ) {
                                        Icon(
                                            imageVector = MiuixIcons.Close,
                                            contentDescription = stringResource(id = R.string.delete),
                                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!expanded && subHeader != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        // 横屏时缺口/导航栏 insets 在左右，分类条必须与搜索栏用同一组水平 insets
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal))
                        .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                        .offset {
                            val inputFieldOffset = with(density) { (SearchBarDefaults.InputFieldMinHeight + 16.dp).roundToPx() }
                            IntOffset(0, searchBarOffsetY() + inputFieldOffset)
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
                                        contentDescription = stringResource(R.string.more_actions),
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
                                // 滚动兜底只能加在弹层这一侧：同一个 filter() 也会作为 LazyColumn 的
                                // item 渲染，在 item 内嵌 verticalScroll 会因无限高度约束直接抛异常
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

fun wrapTagKeyword(keyword: String, translate: Boolean = false): String = run {
    val tag = keyword.substringAfter(':')
    val prefix = keyword.dropLast(tag.length + 1)
    if (translate) {
        val namespacePrefix = TagNamespace.from(prefix)?.prefix
        val newPrefix = EhTagDatabase.getTranslation(tag = prefix) ?: prefix
        val newTag = EhTagDatabase.getTranslation(namespacePrefix, tag) ?: tag
        "$newPrefix：$newTag"
    } else if (keyword.contains(' ')) {
        "$prefix:\"$tag$\""
    } else {
        "$keyword$"
    }
}

private val TagTerminators = charArrayOf('"', '$')
private val WhitespaceRegex = Regex("\\s+")
private val SearchBarHorizontalPadding = 16.dp
private val M3SearchBarMaxWidth = 720.dp
