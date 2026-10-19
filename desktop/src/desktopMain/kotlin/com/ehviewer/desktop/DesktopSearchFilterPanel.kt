package com.ehviewer.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ehviewer.core.i18n.MR
import com.ehviewer.core.model.GalleryInfo
import com.ehviewer.core.shell.list.FilterActionSpec
import com.ehviewer.core.shell.list.FilterDropdownSpec
import com.ehviewer.core.shell.list.FilterToggleSpec
import com.ehviewer.core.shell.list.SearchFilterPanel
import dev.icerock.moko.resources.compose.stringResource
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 桌面在线搜索过滤面板：共享 SearchFilterPanel 的桌面数据面宿主。
// 语义与移动端一致——面板内改动只更新状态不触发搜索，Enter 提交查询串时经 DesktopSearchUrl 生效。
@Composable
fun DesktopSearchFilterPanel(
    filters: DesktopSearchFilters,
    onFiltersChange: (DesktopSearchFilters) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPagesDialog by remember { mutableStateOf(false) }

    val allLabel = stringResource(MR.strings.category_all)
    val languageLabel = stringResource(MR.strings.key_language)
    val anyLabel = stringResource(MR.strings.any)
    val minRatingLabel = stringResource(MR.strings.search_sr)
    val star2 = stringResource(MR.strings.star_2)
    val star3 = stringResource(MR.strings.star_3)
    val star4 = stringResource(MR.strings.star_4)
    val star5 = stringResource(MR.strings.star_5)
    val ratingItems = remember(anyLabel, star2, star3, star4, star5) {
        listOf(anyLabel, star2, star3, star4, star5)
    }
    // 语言候选 = any + S_LANG_TAGS 原文（"en:English" 取冒号后段），与移动端未启用翻译时的形态一致
    val languageItems = remember(anyLabel) {
        List(GalleryInfo.S_LANG_TAGS.size + 1) { i ->
            if (i == 0) anyLabel else GalleryInfo.S_LANG_TAGS[i - 1].substringAfter(':')
        }
    }
    val pagesLabel = stringResource(MR.strings.key_pages)
    val pagesText = remember(filters.pageFrom, filters.pageTo, pagesLabel) {
        desktopPagesText(filters.pageFrom, filters.pageTo, pagesLabel)
    }
    val shLabel = stringResource(MR.strings.search_sh)
    val stoLabel = stringResource(MR.strings.search_sto)
    val disableFilterLabel = stringResource(MR.strings.search_sf)
    val sflLabel = disableFilterLabel + stringResource(MR.strings.search_sfl)
    val sfuLabel = disableFilterLabel + stringResource(MR.strings.search_sfu)
    val sftLabel = disableFilterLabel + stringResource(MR.strings.search_sft)

    SearchFilterPanel(
        categories = DesktopCategoryChips.specs(),
        selectedCategory = filters.category,
        allCategoriesLabel = allLabel,
        onCategoryChange = { onFiltersChange(filters.copy(category = it)) },
        dropdowns = listOf(
            FilterDropdownSpec(
                label = languageLabel,
                items = languageItems,
                // Clamp 持久化恢复值，语言表收缩时下拉下标不越界
                selectedIndex = (filters.language + 1).coerceIn(0, languageItems.lastIndex),
                onSelectIndex = { onFiltersChange(filters.copy(language = it - 1)) },
            ),
            FilterDropdownSpec(
                label = minRatingLabel,
                items = ratingItems,
                selectedIndex = (filters.minRating - 1).coerceIn(0, ratingItems.lastIndex),
                onSelectIndex = { onFiltersChange(filters.copy(minRating = if (it == 0) 0 else it + 1)) },
            ),
        ),
        actions = listOf(
            FilterActionSpec(
                label = pagesText,
                selected = filters.pageFrom != 0 || filters.pageTo != 0,
                onClick = { showPagesDialog = true },
            ),
        ),
        toggles = listOf(
            FilterToggleSpec(
                label = shLabel,
                selected = filters.advanceSearch and DesktopAdvanceSearchBits.SH != 0,
                onToggle = { onFiltersChange(filters.copy(advanceSearch = filters.advanceSearch xor DesktopAdvanceSearchBits.SH)) },
            ),
            FilterToggleSpec(
                label = stoLabel,
                selected = filters.advanceSearch and DesktopAdvanceSearchBits.STO != 0,
                onToggle = { onFiltersChange(filters.copy(advanceSearch = filters.advanceSearch xor DesktopAdvanceSearchBits.STO)) },
            ),
            FilterToggleSpec(
                label = sflLabel,
                selected = filters.advanceSearch and DesktopAdvanceSearchBits.SFL != 0,
                onToggle = { onFiltersChange(filters.copy(advanceSearch = filters.advanceSearch xor DesktopAdvanceSearchBits.SFL)) },
            ),
            FilterToggleSpec(
                label = sfuLabel,
                selected = filters.advanceSearch and DesktopAdvanceSearchBits.SFU != 0,
                onToggle = { onFiltersChange(filters.copy(advanceSearch = filters.advanceSearch xor DesktopAdvanceSearchBits.SFU)) },
            ),
            FilterToggleSpec(
                label = sftLabel,
                selected = filters.advanceSearch and DesktopAdvanceSearchBits.SFT != 0,
                onToggle = { onFiltersChange(filters.copy(advanceSearch = filters.advanceSearch xor DesktopAdvanceSearchBits.SFT)) },
            ),
        ),
        modifier = modifier,
    )

    if (showPagesDialog) {
        DesktopPagesDialog(
            initialFrom = filters.pageFrom,
            initialTo = filters.pageTo,
            onDismiss = { showPagesDialog = false },
            onConfirm = { from, to ->
                showPagesDialog = false
                onFiltersChange(filters.copy(pageFrom = from, pageTo = to))
            },
        )
    }
}

// 页数区间文案：与移动端 pagesText 同形（"a - b P" / "a+ P" / "b- P" / 「Pages」占位）
private fun desktopPagesText(from: Int, to: Int, fallback: String): String = when {
    from > 0 && to > 0 -> "$from - $to P"
    from > 0 -> "$from+ P"
    to > 0 -> "$to- P"
    else -> fallback
}

// 页数区间对话框：与移动端 awaitResult 表单同语义（0-1000 / 0-2000 截断，max!=0 时 min<=min(max/2,max-20) 或 max>=10）
@Composable
private fun DesktopPagesDialog(
    initialFrom: Int,
    initialTo: Int,
    onDismiss: () -> Unit,
    onConfirm: (from: Int, to: Int) -> Unit,
) {
    val title = stringResource(MR.strings.key_pages)
    val toLabel = stringResource(MR.strings.search_sp_to)
    val err1 = stringResource(MR.strings.search_sp_err1)
    val err2 = stringResource(MR.strings.search_sp_err2)
    val cancelLabel = stringResource(MR.strings.desktop_action_cancel)
    val confirmLabel = stringResource(MR.strings.action_save)
    var fromText by remember { mutableStateOf(initialFrom.takeIf { it > 0 }?.toString().orEmpty()) }
    var toText by remember { mutableStateOf(initialTo.takeIf { it > 0 }?.toString().orEmpty()) }
    val from = fromText.toIntOrNull()?.coerceIn(0, 1000) ?: 0
    val to = toText.toIntOrNull()?.coerceIn(0, 2000) ?: 0
    val error = when {
        to != 0 && from != 0 && from > (to / 2).coerceAtMost(to - 20) -> err2
        to != 0 && from == 0 && to < 10 -> err1
        else -> null
    }

    DesktopModalCard(
        title = title,
        onDismiss = onDismiss,
        cardWidth = 440.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TextField(
                value = fromText,
                onValueChange = { fromText = it.filter(Char::isDigit).take(4) },
                modifier = Modifier.width(100.dp).padding(8.dp),
                singleLine = true,
            )
            Text(text = toLabel, color = MiuixTheme.colorScheme.onSurface)
            TextField(
                value = toText,
                onValueChange = { toText = it.filter(Char::isDigit).take(4) },
                modifier = Modifier.width(100.dp).padding(8.dp),
                singleLine = true,
            )
        }
        if (error != null) {
            Text(
                text = error,
                color = MiuixTheme.colorScheme.error,
                fontSize = 12.sp,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DesktopHoverPill(
                onClick = onDismiss,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    text = cancelLabel,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 13.sp,
                )
            }
            DesktopHoverPill(
                enabled = error == null,
                onClick = { onConfirm(from, to) },
                containerColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.15f),
                hoverOverlayColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.12f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 7.dp),
            ) {
                Text(
                    text = confirmLabel,
                    color = if (error == null) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
