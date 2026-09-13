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
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.ehviewer.core.ui.component.SquircleShape
import com.ehviewer.core.ui.icons.EhIcons
import com.ehviewer.core.ui.icons.filled.MenuBook
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
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme

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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val subHeaderHeight = if (subHeader != null) 40.dp else 0.dp
        Scaffold(
            topBar = {
                Column {
                    val scrim = MiuixTheme.colorScheme.background.scrim()
                    Box(Modifier.windowInsetsTopHeight(WindowInsets.statusBars).fillMaxWidth().background(scrim))

                    // Placeholder, fill immutable SearchBar padding
                    Spacer(modifier = Modifier.height(SearchBarDefaults.InputFieldMinHeight + 16.dp + subHeaderHeight))
                }
            },
            floatingActionButton = floatingActionButton,
            content = content,
        )
        // https://issuetracker.google.com/337191298
        // Workaround for can't exit SearchBar due to refocus in non-touch mode
        Box(Modifier.size(1.dp).focusable())
        val activeState = rememberCompositionActiveState()
        val query = searchFieldState.text.toString()
        val contentActive by activeState.state
        val placeholder = title.takeUnless { expanded || contentActive } ?: searchFieldHint
        val searchBg = MiuixTheme.colorScheme.background
        SearchBar(
            modifier = Modifier.align(Alignment.TopCenter)
                .thenIf(!expanded) { offset { IntOffset(0, searchBarOffsetY()) } }
                .thenIf(expanded) { fillMaxSize().background(searchBg) }
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)),
            inputField = {
                InputField(
                    query = query,
                    onQueryChange = { searchFieldState.setTextAndPlaceCursorAtEnd(it) },
                    onSearch = {
                        hideSearchView()
                        onApplySearch()
                    },
                    expanded = expanded,
                    onExpandedChange = onExpandedChange,
                    modifier = Modifier.widthIn(max = (maxWidth - SearchBarHorizontalPadding * 2).coerceAtMost(M3SearchBarMaxWidth)).fillMaxWidth(),
                    label = placeholder,
                    color = if (expanded) MiuixTheme.colorScheme.surfaceContainer else MiuixTheme.colorScheme.surfaceContainerHigh,
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
                            Row {
                                trailingIcon()
                            }
                        }
                    },
                )
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
            filter?.invoke()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues(),
            ) {
                item {}
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
        if (!expanded && subHeader != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset {
                        val inputFieldOffset = with(density) { (SearchBarDefaults.InputFieldMinHeight + 16.dp).roundToPx() }
                        IntOffset(0, searchBarOffsetY() + inputFieldOffset)
                    }
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .fillMaxWidth(),
            ) {
                subHeader()
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
