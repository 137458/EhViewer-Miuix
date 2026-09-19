package com.hippo.ehviewer.ui.tools

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.right
import com.ehviewer.core.i18n.R
import com.ehviewer.core.ui.component.FastScrollLazyColumn
import com.ehviewer.core.ui.component.LabeledCheckbox
import com.ehviewer.core.ui.component.SquircleShape
import com.ehviewer.core.ui.util.AdaptiveLayoutPolicy
import com.ehviewer.core.ui.util.ifNotNullThen
import com.ehviewer.core.ui.util.ifTrueThen
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.client.EhTagDatabase.suggestion
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.Checkbox as MiuixCheckbox
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.RadioButton as MiuixRadioButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Create
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.layout.DialogDefaults as MiuixDialogDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

fun interface ActionScope {
    fun onSelect(action: String, that: suspend () -> Unit)
}

/**
 * 弹层是否必须走紧凑形态。
 *
 * Miuix 的弹层只在「宽 ≥840dp 且高 ≥480dp」时限制内容高度，横屏手机（如 800x412dp）
 * 下不受限，过高的内容会被窗口直接裁切。
 */
@Composable
private fun isCompactDialogLayout(): Boolean {
    val configuration = LocalConfiguration.current
    return AdaptiveLayoutPolicy.isShortLandscape(configuration.screenWidthDp, configuration.screenHeightDp)
}

interface DialogScope<R> {
    var expectedValue: R
}

typealias MutableComposable = MutableState<(@Composable BoxScope.() -> Unit)?>

class DialogState(val mutex: MutatorMutex = MutatorMutex()) : MutableComposable by mutableStateOf(null) {
    @Composable
    fun rememberLocal() = remember { DialogState(mutex) }

    @Composable
    context(scope: BoxScope)
    fun Place() = value?.let { it(scope) }

    fun dismiss() {
        value = null
    }
}

context(state: DialogState)
suspend inline fun <R> dialog(crossinline block: @Composable BoxScope.(CancellableContinuation<R>) -> Unit) = state.mutex.mutate {
    try {
        suspendCancellableCoroutine { cont -> state.value = { block(cont) } }
    } finally {
        state.dismiss()
    }
}

context(_: DialogState)
suspend fun <R> awaitResult(
    initial: R,
    @StringRes title: Int? = null,
    invalidator: (suspend Raise<String>.(R) -> Unit)? = null,
    block: @Composable DialogScope<R>.(String?) -> Unit,
): R = dialog { cont ->
    val state = remember(cont) { mutableStateOf(initial) }
    var errorMsg by remember(cont) { mutableStateOf<String?>(null) }
    val impl = remember(cont) {
        object : DialogScope<R> {
            override var expectedValue by state
        }
    }
    if (invalidator != null) {
        LaunchedEffect(state) {
            snapshotFlow { state.value }.collectLatest {
                errorMsg = either { invalidator(it) }.leftOrNull()
            }
        }
    }
    WindowDialog(
        show = true,
        onDismissRequest = { cont.cancel() },
        title = title?.let { stringResource(id = it) },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            block(impl, errorMsg)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MiuixTextButton(
                    text = stringResource(id = android.R.string.cancel),
                    onClick = { cont.cancel() },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(16.dp))
                MiuixTextButton(
                    text = stringResource(id = android.R.string.ok),
                    onClick = {
                        if (invalidator == null || errorMsg == null) {
                            cont.resume(state.value)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = MiuixButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}

context(_: Context, _: DialogState)
suspend fun awaitSelectTags(): List<String> = dialog { cont ->
    val selected = remember { mutableStateListOf<String>() }
    val state = rememberTextFieldState()
    var suggestionTranslate by rememberMutableStateInDataStore("SuggestionTranslate") { false }
    PausableAlertDialog(
        confirmButton = {
            MiuixTextButton(
                text = stringResource(id = android.R.string.ok),
                onClick = { cont.resume(selected.toList()) },
            )
        },
        dismissButton = {
            MiuixTextButton(
                text = stringResource(id = android.R.string.cancel),
                onClick = { cont.cancel() },
            )
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiuixText(text = stringResource(id = R.string.action_add_tag))
                if (EhTagDatabase.translatable) {
                    Spacer(modifier = Modifier.weight(1f))
                    MiuixText(
                        text = stringResource(id = R.string.translate_tag_for_tagger),
                        style = MiuixTheme.textStyles.body2,
                    )
                    MiuixCheckbox(
                        state = ToggleableState(suggestionTranslate),
                        onClick = { suggestionTranslate = !suggestionTranslate },
                    )
                }
            }
        },
        text = {
            Column {
                if (selected.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        selected.forEach { text ->
                            Row(
                                modifier = Modifier
                                    .clip(SquircleShape(8.dp))
                                    .background(MiuixTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                MiuixText(
                                    text = text,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = MiuixTheme.colorScheme.onPrimaryContainer,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                MiuixIcon(
                                    imageVector = MiuixIcons.Close,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { selected -= text },
                                )
                            }
                        }
                    }
                }
                MiuixTextField(
                    state = state,
                    label = stringResource(id = R.string.action_add_tag_tip),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val text = state.text.toString().trim()
                                if (text.isNotEmpty()) {
                                    selected += text
                                    state.clearText()
                                }
                            },
                        ) {
                            MiuixIcon(
                                imageVector = MiuixIcons.Add,
                                contentDescription = stringResource(id = R.string.action_add_tag),
                            )
                        }
                    },
                )
                val query = state.text.toString().trim().takeIf { s -> s.isNotEmpty() }
                var items by remember { mutableStateOf(emptyList<EhTagDatabase.Tag>()) }
                LaunchedEffect(suggestionTranslate, query) {
                    items = query?.let { suggestion(query, suggestionTranslate).take(15) }.orEmpty()
                }
                if (items.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .clip(SquircleShape(8.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainer),
                    ) {
                        items(items.size) { i ->
                            val (tag, hint) = items[i]
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (tag.endsWith(':')) {
                                            state.setTextAndPlaceCursorAtEnd(tag)
                                        } else {
                                            selected += tag
                                            state.clearText()
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                MiuixText(
                                    text = tag,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 2,
                                    style = MiuixTheme.textStyles.body2,
                                )
                                if (hint != null) {
                                    MiuixText(
                                        text = hint,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1,
                                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                        style = MiuixTheme.textStyles.footnote1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        idleIcon = MiuixIcons.Create,
    )
}

context(_: DialogState)
suspend fun awaitInputText(
    initial: String = "",
    title: String? = null,
    hint: String? = null,
    isNumber: Boolean = false,
    @StringRes confirmText: Int = android.R.string.ok,
    onUserDismiss: (() -> Unit)? = null,
    invalidator: (suspend Raise<String>.(String) -> Unit)? = null,
) = dialog { cont ->
    val coroutineScope = rememberCoroutineScope()
    var text by remember(cont) { mutableStateOf(initial) }
    var error by remember(cont) { mutableStateOf<String?>(null) }
    WindowDialog(
        show = true,
        onDismissRequest = {
            cont.cancel()
            onUserDismiss?.invoke()
        },
        title = title,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                MiuixTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        error = null
                    },
                    label = hint.orEmpty(),
                    singleLine = true,
                    keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    MiuixText(
                        text = error!!,
                        color = MiuixTheme.colorScheme.error,
                        style = MiuixTheme.textStyles.body2,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MiuixTextButton(
                    text = stringResource(id = android.R.string.cancel),
                    onClick = {
                        cont.cancel()
                        onUserDismiss?.invoke()
                    },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(16.dp))
                MiuixTextButton(
                    text = stringResource(id = confirmText),
                    onClick = {
                        if (invalidator == null) {
                            cont.resume(text)
                        } else {
                            coroutineScope.launch {
                                error = either { invalidator(text) }.leftOrNull()
                                if (error == null) cont.resume(text)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = MiuixButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}

context(_: DialogState)
suspend fun awaitInputTextWithCheckBox(
    initial: String = "",
    @StringRes title: Int? = null,
    @StringRes hint: Int? = null,
    checked: Boolean,
    @StringRes checkBoxText: Int,
    isNumber: Boolean = false,
    invalidator: (suspend Raise<String>.(String, Boolean) -> Unit)? = null,
): Pair<String, Boolean> = dialog { cont ->
    val coroutineScope = rememberCoroutineScope()
    var text by remember(cont) { mutableStateOf(initial) }
    var error by remember(cont) { mutableStateOf<String?>(null) }
    var checkedState by remember { mutableStateOf(checked) }
    WindowDialog(
        show = true,
        onDismissRequest = { cont.cancel() },
        title = title?.let { stringResource(id = it) },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                MiuixTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        error = null
                    },
                    label = hint?.let { stringResource(id = it) }.orEmpty(),
                    singleLine = true,
                    keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    MiuixText(
                        text = error!!,
                        color = MiuixTheme.colorScheme.error,
                        style = MiuixTheme.textStyles.body2,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                    )
                }
            }
            LabeledCheckbox(
                modifier = Modifier.fillMaxWidth(),
                checked = checkedState,
                onCheckedChange = { checkedState = !checkedState },
                label = stringResource(checkBoxText),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MiuixTextButton(
                    text = stringResource(id = android.R.string.cancel),
                    onClick = { cont.cancel() },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(16.dp))
                MiuixTextButton(
                    text = stringResource(id = android.R.string.ok),
                    onClick = {
                        if (invalidator == null) {
                            cont.resume(text to checkedState)
                        } else {
                            coroutineScope.launch {
                                error = either { invalidator(text, checkedState) }.leftOrNull()
                                if (error == null) cont.resume(text to checkedState)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = MiuixButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}

context(_: DialogState)
suspend fun awaitConfirmationOrCancel(
    @StringRes confirmText: Int = android.R.string.ok,
    @StringRes dismissText: Int = android.R.string.cancel,
    @StringRes title: Int? = null,
    showConfirmButton: Boolean = true,
    showCancelButton: Boolean = true,
    onCancelButtonClick: () -> Unit = {},
    secure: Boolean = false,
    text: @Composable (() -> Unit)? = null,
) = dialog<Unit> { cont ->
    WindowDialog(
        show = true,
        onDismissRequest = { cont.cancel() },
        title = title?.let { stringResource(id = it) },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (text != null) {
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    text()
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (showCancelButton) {
                    MiuixTextButton(
                        text = stringResource(id = dismissText),
                        onClick = {
                            onCancelButtonClick()
                            cont.cancel()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (showCancelButton && showConfirmButton) {
                    Spacer(Modifier.width(16.dp))
                }
                if (showConfirmButton) {
                    MiuixTextButton(
                        text = stringResource(id = confirmText),
                        onClick = { cont.resume(Unit) },
                        modifier = Modifier.weight(1f),
                        colors = MiuixButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }
        }
    }
}

context(_: DialogState)
suspend fun awaitSelectDate(
    @StringRes title: Int,
    initialSelectedDateMillis: Long? = null,
    initialDisplayedMonthMillis: Long? = initialSelectedDateMillis,
    yearRange: IntRange = DatePickerDefaults.YearRange,
    initialDisplayMode: DisplayMode = DisplayMode.Picker,
    selectableDates: SelectableDates = DatePickerDefaults.AllDates,
    showModeToggle: Boolean = true,
): Long = dialog { cont ->
    // 横屏矮视口放不下 568dp 高的日历选择器，改用紧凑的输入模式避免被窗口裁切
    val displayMode = if (isCompactDialogLayout()) DisplayMode.Input else initialDisplayMode
    val state = rememberDatePickerState(
        initialSelectedDateMillis,
        initialDisplayedMonthMillis,
        yearRange,
        displayMode,
        selectableDates,
    )
    WindowDialog(
        show = true,
        onDismissRequest = { cont.cancel() },
        maxWidth = 560.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            DatePicker(
                state = state,
                title = {
                    MiuixText(
                        text = stringResource(id = title),
                        modifier = Modifier.padding(DatePickerTitlePadding),
                        style = MiuixTheme.textStyles.title4,
                    )
                },
                showModeToggle = showModeToggle,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                MiuixTextButton(
                    text = stringResource(id = android.R.string.cancel),
                    onClick = { cont.cancel() },
                )
                Spacer(modifier = Modifier.width(8.dp))
                MiuixTextButton(
                    text = stringResource(id = android.R.string.ok),
                    onClick = { state.selectedDateMillis?.let { cont.resume(it) } ?: cont.cancel() },
                )
            }
        }
    }
}

context(_: DialogState)
suspend fun <R> showNoButton(respectDefaultWidth: Boolean = true, block: @Composable Continuation<R>.() -> Unit): R = dialog { cont ->
    WindowDialog(
        show = true,
        onDismissRequest = { cont.cancel() },
        maxWidth = if (!respectDefaultWidth) 560.dp else MiuixDialogDefaults.MaxWidth,
    ) {
        block(cont)
    }
}

context(_: DialogState)
suspend fun awaitSelectTime(
    title: String,
    initialHour: Int,
    initialMinute: Int,
) = dialog { cont ->
    val state = rememberTimePickerState(initialHour, initialMinute)
    WindowDialog(
        show = true,
        onDismissRequest = { cont.cancel() },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MiuixText(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                text = title,
                style = MiuixTheme.textStyles.title4,
            )
            // 横屏矮视口放不下表盘时钟，改用紧凑的输入模式
            if (isCompactDialogLayout()) {
                TimeInput(state = state)
            } else {
                TimePicker(state = state)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                MiuixTextButton(
                    text = stringResource(id = android.R.string.cancel),
                    onClick = { cont.cancel() },
                )
                Spacer(modifier = Modifier.width(8.dp))
                MiuixTextButton(
                    text = stringResource(id = android.R.string.ok),
                    onClick = { cont.resume(state.hour to state.minute) },
                )
            }
        }
    }
}

context(_: DialogState)
suspend fun awaitSingleChoice(
    items: List<String>,
    selected: Int,
    @StringRes title: Int? = null,
): Int = showNoButton {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
        title?.let {
            MiuixText(
                text = stringResource(id = it),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                style = MiuixTheme.textStyles.title4,
            )
        }
        items.forEachIndexed { index, text ->
            BasicComponent(
                startAction = {
                    MiuixRadioButton(selected = index == selected, onClick = null)
                },
                onClick = { resume(index) },
                role = Role.RadioButton,
                holdDownState = index == selected,
            ) {
                MiuixText(text = text, style = MiuixTheme.textStyles.body1)
            }
        }
    }
}

context(_: DialogState)
suspend fun awaitSelectItem(
    items: List<String>,
    @StringRes title: Int? = null,
    selected: Int = -1,
    respectDefaultWidth: Boolean = true,
) = awaitSelectItem(items, title?.right(), selected, respectDefaultWidth)

context(_: DialogState)
suspend fun awaitSelectItem(
    items: List<String>,
    title: Either<String, Int>?,
    selected: Int = -1,
    respectDefaultWidth: Boolean = true,
): Int = showNoButton(respectDefaultWidth) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        if (title != null) {
            MiuixText(
                text = title.fold({ it }, { stringResource(id = it) }),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                style = MiuixTheme.textStyles.title4,
            )
        }
        FastScrollLazyColumn {
            itemsIndexed(items) { index, text ->
                BasicComponent(
                    onClick = { resume(index) },
                    role = Role.RadioButton,
                    holdDownState = index == selected,
                ) {
                    CheckableItem(
                        text = text,
                        checked = index == selected,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

context(_: DialogState)
suspend inline fun awaitSelectAction(
    @StringRes title: Int? = null,
    selected: Int = -1,
    builder: ActionScope.() -> Unit,
): suspend () -> Unit {
    val (items, actions) = buildList { builder { action, that -> add(action to that) } }.unzip()
    val index = awaitSelectItem(items, title, selected)
    return actions[index]
}

context(_: DialogState)
suspend fun awaitSelectItemWithCheckBox(
    items: List<String>,
    @StringRes title: Int,
    @StringRes checkBoxText: Int,
    selected: Int = -1,
    initialChecked: Boolean = false,
): Pair<Int, Boolean> = showNoButton {
    var checked by remember { mutableStateOf(initialChecked) }
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        MiuixText(
            text = stringResource(id = title),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            style = MiuixTheme.textStyles.title4,
        )
        FastScrollLazyColumn {
            itemsIndexed(items) { index, text ->
                BasicComponent(
                    onClick = { resume(index to checked) },
                    role = Role.RadioButton,
                    holdDownState = index == selected,
                ) {
                    CheckableItem(
                        text = text,
                        checked = index == selected,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        LabeledCheckbox(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            checked = checked,
            onCheckedChange = { checked = !checked },
            label = stringResource(checkBoxText),
        )
    }
}

context(_: DialogState)
suspend fun awaitSelectItemWithIcon(
    items: List<Pair<ImageVector, Int>>,
    title: String,
): Int = showNoButton {
    LazyColumn {
        stickyHeader {
            MiuixText(
                text = title,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                style = MiuixTheme.textStyles.title4,
            )
        }
        itemsIndexed(items) { index, (icon, text) ->
            BasicComponent(
                startAction = {
                    MiuixIcon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurface,
                    )
                },
                onClick = { resume(index) },
                role = Role.Button,
                holdDownState = false,
            ) {
                MiuixText(
                    text = stringResource(id = text),
                    style = MiuixTheme.textStyles.body1,
                )
            }
        }
    }
}

context(_: DialogState)
suspend fun awaitSelectItemWithIconAndTextField(
    items: List<Pair<ImageVector, String>>,
    @StringRes title: Int,
    @StringRes hint: Int,
    initialNote: String,
    maxChar: Int,
): Pair<Int, String> = showNoButton(false) {
    Column {
        MiuixText(
            text = stringResource(id = title),
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp),
            style = MiuixTheme.textStyles.title4,
        )
        // 圆形布局是正方形，横屏矮视口下按窗口高度收缩，避免超出窗口被裁切
        CircularLayout(
            modifier = Modifier
                .fillMaxWidth(if (isCompactDialogLayout()) 0.5F else 1F)
                .aspectRatio(1F),
            placeFirstItemInCenter = true,
        ) {
            val note = rememberTextFieldState(initialNote)
            MiuixTextField(
                state = note,
                modifier = Modifier.fillMaxWidth(0.45F).aspectRatio(1F),
                label = stringResource(id = hint),
                trailingIcon = {
                    if (note.text.isNotEmpty()) {
                        IconButton(onClick = { note.clearText() }) {
                            MiuixIcon(
                                imageVector = MiuixIcons.Close,
                                contentDescription = stringResource(id = R.string.clear_all),
                            )
                        }
                    }
                },
            )
            items.forEachIndexed { index, (icon, text) ->
                Column(
                    modifier = Modifier
                        .clip(IconWithTextCorner)
                        .clickable(role = Role.Button) { resume(index to note.text.toString()) }
                        .fillMaxWidth(0.2F),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    MiuixIcon(imageVector = icon, contentDescription = null, tint = MiuixTheme.colorScheme.onSurface)
                    MiuixText(
                        text = text,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2,
                        style = MiuixTheme.textStyles.footnote1,
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckableItem(text: String, checked: Boolean, modifier: Modifier = Modifier) {
    val textStyle = MiuixTheme.textStyles.body1
    val checkedColor = MiuixTheme.colorScheme.primary
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiuixText(
            text = text,
            style = if (checked) textStyle.copy(color = checkedColor) else textStyle,
            modifier = Modifier.weight(1f),
        )
        if (checked) {
            MiuixIcon(
                imageVector = MiuixIcons.Ok,
                contentDescription = null,
                tint = checkedColor,
            )
        }
    }
}

private val IconWithTextCorner = RoundedCornerShape(8.dp)
private val DatePickerTitlePadding = PaddingValues(start = 24.dp, end = 12.dp, top = 16.dp)

val LocalGlobalDialogState = compositionLocalOf<DialogState> { error("CompositionLocal LocalDialogState not present!") }
