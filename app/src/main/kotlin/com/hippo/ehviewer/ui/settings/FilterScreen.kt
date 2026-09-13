package com.hippo.ehviewer.ui.settings

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.ehviewer.core.database.model.Filter
import com.ehviewer.core.database.model.FilterMode
import com.ehviewer.core.i18n.R
import com.ehviewer.core.ui.component.BlurredBar
import com.ehviewer.core.ui.component.blurBackdropSource
import com.ehviewer.core.ui.component.rememberBlurBackdrop
import com.ehviewer.core.ui.component.DropdownFilterChip
import com.ehviewer.core.ui.util.Await
import com.ehviewer.core.ui.util.thenIf
import com.ehviewer.core.util.async
import com.ehviewer.core.util.launch
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhFilter
import com.hippo.ehviewer.client.EhFilter.forget
import com.hippo.ehviewer.client.EhFilter.remember
import com.hippo.ehviewer.client.EhFilter.trigger
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.main.NavigationIcon
import com.hippo.ehviewer.ui.tools.awaitConfirmationOrCancel
import com.hippo.ehviewer.ui.tools.dialog
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlin.coroutines.resume
import moe.tarsin.coroutines.groupByToObserved
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.icon.extended.Help
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.FilterScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val scrollBehavior = MiuixScrollBehavior()
    val allFilterMap = remember { async { EhFilter.filters.await().groupByToObserved { it.mode } } }
    val textIsEmpty = stringResource(R.string.text_is_empty)
    val labelExist = stringResource(R.string.label_text_exist)
    val animateItems by Settings.animateItems.collectAsState()

    fun addFilter() {
        launch {
            dialog { cont ->
                val types = stringArrayResource(id = com.hippo.ehviewer.R.array.filter_entries)
                var selectedTypeIndex by remember { mutableStateOf(0) }
                val state = rememberTextFieldState()
                var error by remember { mutableStateOf<String?>(null) }
                fun invalidateAndSave() {
                    if (state.text.isBlank()) {
                        error = textIsEmpty
                        return
                    }
                    error = null
                    val mode = FilterMode.entries[selectedTypeIndex]
                    val filter = Filter(mode, state.text.toString())
                    filter.remember {
                        if (it) {
                            cont.resume(Unit)
                            requireNotNull(allFilterMap.getCompleted()[mode]).add(filter)
                        } else {
                            error = labelExist
                        }
                    }
                }
                WindowDialog(
                    show = true,
                    onDismissRequest = { cont.cancel() },
                    title = stringResource(id = R.string.add_filter),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        DropdownFilterChip(
                            label = stringResource(id = R.string.filter_label),
                            menuItems = types.toList(),
                            selectedItemIndex = selectedTypeIndex,
                            onSelectedItemIndexChange = { selectedTypeIndex = it },
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                        TextField(
                            state = state,
                            label = stringResource(id = R.string.filter_text),
                            lineLimits = TextFieldLineLimits.SingleLine,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done,
                            ),
                            trailingIcon = {
                                if (error != null) {
                                    Icon(
                                        imageVector = MiuixIcons.Info,
                                        contentDescription = null,
                                        tint = MiuixTheme.colorScheme.error,
                                    )
                                }
                            },
                        )
                        if (error != null) {
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = error!!,
                                color = MiuixTheme.colorScheme.error,
                                style = MiuixTheme.textStyles.footnote1,
                            )
                        }
                        Spacer(modifier = Modifier.size(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(
                                text = stringResource(id = android.R.string.cancel),
                                onClick = { cont.cancel() },
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            TextButton(
                                text = stringResource(id = R.string.add),
                                onClick = ::invalidateAndSave,
                            )
                        }
                    }
                }
            }
        }
    }

    val backdrop = rememberBlurBackdrop()

    Scaffold(
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
            ) {
                TopAppBar(
                    title = stringResource(id = R.string.filter),
                    navigationIcon = { NavigationIcon() },
                    actions = {
                        IconButton(
                            onClick = {
                                launch {
                                    awaitConfirmationOrCancel(
                                        title = R.string.filter,
                                        showCancelButton = false,
                                    ) {
                                        Text(text = stringResource(id = R.string.filter_tip))
                                    }
                                }
                            },
                        ) {
                            Icon(imageVector = MiuixIcons.Help, contentDescription = null)
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    color = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface,
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = ::addFilter) {
                Icon(imageVector = MiuixIcons.Add, contentDescription = null)
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background)
                .blurBackdropSource(backdrop),
            contentAlignment = Alignment.TopCenter,
        ) {
            Await({ allFilterMap.await() }) { filters ->
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxHeight()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = paddingValues,
                ) {
                    var showTip = true
                    filters.forEach { (filterMode, filterList) ->
                        val title = when (filterMode) {
                            FilterMode.TITLE -> R.string.filter_title
                            FilterMode.UPLOADER -> R.string.filter_uploader
                            FilterMode.TAG -> R.string.filter_tag
                            FilterMode.TAG_NAMESPACE -> R.string.filter_tag_namespace
                            FilterMode.COMMENTER -> R.string.filter_commenter
                            FilterMode.COMMENT -> R.string.filter_comment
                        }
                        if (filterList.isNotEmpty()) {
                            item(key = "title_$filterMode") {
                                SmallTitle(
                                    text = stringResource(id = title),
                                    modifier = Modifier.thenIf(animateItems) { animateItem() },
                                )
                            }
                            item(key = "card_$filterMode") {
                                Card(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .thenIf(animateItems) { animateItem() },
                                ) {
                                    filterList.forEach { filter ->
                                        val filterCheckBoxRecomposeScope = currentRecomposeScope
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .triStateToggleable(
                                                    state = ToggleableState(filter.enable),
                                                    onClick = { filter.trigger { filterCheckBoxRecomposeScope.invalidate() } },
                                                )
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Checkbox(
                                                state = ToggleableState(filter.enable),
                                                onClick = null,
                                            )
                                            Spacer(modifier = Modifier.size(12.dp))
                                            Text(text = filter.text, modifier = Modifier.weight(1F))
                                            IconButton(
                                                onClick = {
                                                    launch {
                                                        awaitConfirmationOrCancel(confirmText = R.string.delete) {
                                                            Text(text = stringResource(id = R.string.delete_filter, filter.text))
                                                        }
                                                        filter.forget {
                                                            filterList.remove(filter)
                                                        }
                                                    }
                                                },
                                            ) {
                                                Icon(imageVector = MiuixIcons.Delete, contentDescription = stringResource(id = R.string.delete))
                                            }
                                        }
                                    }
                                }
                            }
                            showTip = false
                        }
                    }
                    if (showTip) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Spacer(modifier = Modifier.size(80.dp))
                                Icon(
                                    imageVector = MiuixIcons.Filter,
                                    contentDescription = null,
                                    modifier = Modifier.padding(16.dp).size(120.dp),
                                    tint = MiuixTheme.colorScheme.primary,
                                )
                                Text(
                                    text = stringResource(id = R.string.filter),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
