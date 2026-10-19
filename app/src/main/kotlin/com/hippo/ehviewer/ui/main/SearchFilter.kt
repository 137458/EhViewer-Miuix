package com.hippo.ehviewer.ui.main

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import arrow.core.raise.ensure
import com.ehviewer.core.i18n.R
import com.ehviewer.core.model.GalleryInfo.Companion.S_LANG_TAGS
import com.ehviewer.core.shell.list.CategoryChipSpec
import com.ehviewer.core.shell.list.FilterActionSpec
import com.ehviewer.core.shell.list.FilterDropdownSpec
import com.ehviewer.core.shell.list.FilterToggleSpec
import com.ehviewer.core.shell.list.SearchFilterPanel
import com.ehviewer.core.util.launch
import com.ehviewer.core.util.toIntOrDefault
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.ui.tools.awaitResult
import kotlinx.coroutines.CoroutineScope
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val categoryTable = arrayOf(
    EhUtils.DOUJINSHI to R.string.doujinshi,
    EhUtils.MANGA to R.string.manga,
    EhUtils.ARTIST_CG to R.string.artist_cg,
    EhUtils.GAME_CG to R.string.game_cg,
    EhUtils.WESTERN to R.string.western,
    EhUtils.NON_H to R.string.non_h,
    EhUtils.IMAGE_SET to R.string.image_set,
    EhUtils.COSPLAY to R.string.cosplay,
    EhUtils.ASIAN_PORN to R.string.asian_porn,
    EhUtils.MISC to R.string.misc,
)

// 分类胶囊数据面：搜索展开视图分类条与筛选面板共用（flag/颜色与 EhUtils 同源）
@Composable
fun galleryCategoryChips(): List<CategoryChipSpec> = categoryTable.map { (cat, stringRes) ->
    CategoryChipSpec(
        flag = cat,
        label = stringResource(stringRes),
        color = EhUtils.getCategoryColor(cat),
    )
}

// 筛选面板薄宿主：UI 骨架由共享 SearchFilterPanel 承载，此处仅注入数据面
// （语言翻译、最低评分候选、页数编辑对话框与高级开关位）。
@Composable
context(_: DialogState, _: CoroutineScope, _: Context)
fun SearchFilter(
    modifier: Modifier = Modifier,
    category: Int,
    onCategoryChange: (Int) -> Unit,
    language: Int,
    onLanguageChange: (Int) -> Unit,
    advancedOption: AdvancedSearchOption,
    onAdvancedOptionChange: (AdvancedSearchOption) -> Unit,
) {
    val any = stringResource(id = R.string.any)
    val languages = remember(any) {
        val translatable = EhTagDatabase.initialized && EhTagDatabase.translatable
        List(S_LANG_TAGS.size + 1) { i ->
            if (i == 0) {
                any
            } else {
                val tag = S_LANG_TAGS[i - 1].substringAfter(':')
                if (translatable) {
                    EhTagDatabase.getTranslation("l", tag) ?: tag
                } else {
                    tag
                }
            }
        }
    }
    val minRatingItems = stringArrayResource(id = com.hippo.ehviewer.R.array.search_min_rating)
    val pageErr1 = stringResource(R.string.search_sp_err1)
    val pageErr2 = stringResource(R.string.search_sp_err2)
    val pages = stringResource(id = R.string.key_pages)
    val pagesText = remember(advancedOption.fromPage, advancedOption.toPage, pages) {
        with(advancedOption) {
            val hasFrom = fromPage > 0
            val hasTo = toPage > 0
            if (hasFrom && hasTo) {
                "$fromPage - $toPage P"
            } else if (hasFrom) {
                "$fromPage+ P"
            } else if (hasTo) {
                "$toPage- P"
            } else {
                pages
            }
        }
    }
    SearchFilterPanel(
        categories = galleryCategoryChips(),
        selectedCategory = category,
        allCategoriesLabel = stringResource(R.string.category_all),
        onCategoryChange = onCategoryChange,
        dropdowns = listOf(
            FilterDropdownSpec(
                label = stringResource(id = R.string.key_language),
                items = languages,
                // Clamp restored preference values so a removed/unknown language cannot make
                // the dropdown index invalid during composition.
                selectedIndex = (language + 1).coerceIn(0, languages.lastIndex),
                onSelectIndex = { onLanguageChange(it - 1) },
            ),
            FilterDropdownSpec(
                label = stringResource(id = R.string.search_sr),
                items = minRatingItems.asList(),
                selectedIndex = (advancedOption.minRating - 1).coerceIn(0, minRatingItems.lastIndex),
                onSelectIndex = {
                    onAdvancedOptionChange(advancedOption.copy(minRating = if (it == 0) 0 else it + 1))
                },
            ),
        ),
        actions = listOf(
            FilterActionSpec(
                label = pagesText,
                selected = advancedOption.fromPage != 0 || advancedOption.toPage != 0,
                onClick = {
                    launch {
                        val (from, to) = awaitResult(
                            initial = advancedOption.fromPage to advancedOption.toPage,
                            title = R.string.key_pages,
                            invalidator = { (min, max) ->
                                if (max != 0) {
                                    if (min != 0) {
                                        ensure(min <= (max / 2).coerceAtMost(max - 20)) { pageErr2 }
                                    } else {
                                        ensure(max >= 10) { pageErr1 }
                                    }
                                }
                            },
                        ) { error ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                ) {
                                    TextField(
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        value = expectedValue.first.takeIf { it > 0 }?.toString().orEmpty(),
                                        onValueChange = {
                                            expectedValue = expectedValue.copy(first = it.toIntOrDefault(0).coerceIn(0, 1000))
                                        },
                                        modifier = Modifier.width(100.dp).padding(8.dp),
                                        singleLine = true,
                                    )
                                    Text(
                                        text = stringResource(id = R.string.search_sp_to),
                                        color = MiuixTheme.colorScheme.onSurface,
                                    )
                                    TextField(
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        value = expectedValue.second.takeIf { it > 0 }?.toString().orEmpty(),
                                        onValueChange = {
                                            expectedValue = expectedValue.copy(second = it.toIntOrDefault(0).coerceIn(0, 2000))
                                        },
                                        modifier = Modifier.width(100.dp).padding(8.dp),
                                        singleLine = true,
                                    )
                                    Text(
                                        text = stringResource(id = R.string.search_sp_suffix),
                                        color = MiuixTheme.colorScheme.onSurface,
                                    )
                                }
                                if (error != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = MiuixIcons.Info,
                                            contentDescription = null,
                                            tint = MiuixTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Text(
                                            text = error,
                                            color = MiuixTheme.colorScheme.error,
                                            style = MiuixTheme.textStyles.body2,
                                        )
                                    }
                                }
                            }
                        }
                        onAdvancedOptionChange(advancedOption.copy(fromPage = from, toPage = to))
                    }
                },
            ),
        ),
        toggles = listOf(
            FilterToggleSpec(
                label = stringResource(id = R.string.search_sh),
                selected = advancedOption.advanceSearch and AdvanceTable.SH != 0,
                onToggle = { onAdvancedOptionChange(advancedOption.copy(advanceSearch = advancedOption.advanceSearch xor AdvanceTable.SH)) },
            ),
            FilterToggleSpec(
                label = stringResource(id = R.string.search_sto),
                selected = advancedOption.advanceSearch and AdvanceTable.STO != 0,
                onToggle = { onAdvancedOptionChange(advancedOption.copy(advanceSearch = advancedOption.advanceSearch xor AdvanceTable.STO)) },
            ),
            FilterToggleSpec(
                label = stringResource(id = R.string.search_sf) + stringResource(id = R.string.search_sfl),
                selected = advancedOption.advanceSearch and AdvanceTable.SFL != 0,
                onToggle = { onAdvancedOptionChange(advancedOption.copy(advanceSearch = advancedOption.advanceSearch xor AdvanceTable.SFL)) },
            ),
            FilterToggleSpec(
                label = stringResource(id = R.string.search_sf) + stringResource(id = R.string.search_sfu),
                selected = advancedOption.advanceSearch and AdvanceTable.SFU != 0,
                onToggle = { onAdvancedOptionChange(advancedOption.copy(advanceSearch = advancedOption.advanceSearch xor AdvanceTable.SFU)) },
            ),
            FilterToggleSpec(
                label = stringResource(id = R.string.search_sf) + stringResource(id = R.string.search_sft),
                selected = advancedOption.advanceSearch and AdvanceTable.SFT != 0,
                onToggle = { onAdvancedOptionChange(advancedOption.copy(advanceSearch = advancedOption.advanceSearch xor AdvanceTable.SFT)) },
            ),
        ),
        modifier = modifier,
    )
}
