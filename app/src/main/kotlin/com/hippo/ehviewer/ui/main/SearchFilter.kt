package com.hippo.ehviewer.ui.main

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import arrow.core.raise.ensure
import com.ehviewer.core.i18n.R
import com.ehviewer.core.model.GalleryInfo.Companion.S_LANG_TAGS
import com.ehviewer.core.ui.component.DropdownFilterChip
import com.ehviewer.core.ui.component.LiquidGlassSurface
import com.ehviewer.core.ui.component.SquircleShape
import com.ehviewer.core.ui.util.thenIf
import com.ehviewer.core.util.launch
import com.ehviewer.core.util.toIntOrDefault
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.collectAsState
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
) = Column(
    modifier = modifier.padding(bottom = 8.dp),
) {
    val haptic = LocalHapticFeedback.current
    val isAllSelected = category == EhUtils.ALL_CATEGORY || category == EhUtils.NONE || category <= 0

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // "全部" 胶囊
        val allBg = if (isAllSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainer
        val allText = if (isAllSelected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface

        LiquidGlassSurface(
            shape = CircleShape,
            containerColor = allBg,
            elevation = if (isAllSelected) 3.dp else 1.dp,
            refractionHeight = 8.dp,
            refractionAmount = 8.dp,
            modifier = Modifier.clickable(role = Role.RadioButton) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCategoryChange(EhUtils.ALL_CATEGORY)
            },
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.category_all),
                    fontSize = 13.sp,
                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                    color = allText,
                )
            }
        }

        // 10 分类胶囊
        categoryTable.forEach { (cat, stringRes) ->
            val isSelected = !isAllSelected && (category and cat != 0)
            val catColor = EhUtils.getCategoryColor(cat)
            val pillBg = if (isSelected) catColor else MiuixTheme.colorScheme.surfaceContainer
            val pillText = if (isSelected) Color.White else MiuixTheme.colorScheme.onSurface

            LiquidGlassSurface(
                shape = CircleShape,
                containerColor = pillBg,
                elevation = if (isSelected) 3.dp else 1.dp,
                refractionHeight = 8.dp,
                refractionAmount = 8.dp,
                modifier = Modifier.clickable(role = Role.RadioButton) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isSelected) {
                        val newCat = category and cat.inv()
                        onCategoryChange(if (newCat == 0) EhUtils.ALL_CATEGORY else newCat)
                    } else {
                        if (isAllSelected) {
                            onCategoryChange(cat)
                        } else {
                            onCategoryChange(category or cat)
                        }
                    }
                },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!isSelected) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(catColor),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = stringResource(stringRes),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = pillText,
                    )
                }
            }
        }
    }
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val any = stringResource(id = R.string.any)
        val languageStr = stringResource(id = R.string.key_language)
        val languages = remember {
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
        DropdownFilterChip(
            label = languageStr,
            menuItems = languages,
            selectedItemIndex = language + 1,
            onSelectedItemIndexChange = { onLanguageChange(it - 1) },
        )
        val minRatingItems = stringArrayResource(id = com.hippo.ehviewer.R.array.search_min_rating)
        val minRatingStr = stringResource(id = R.string.search_sr)
        DropdownFilterChip(
            label = minRatingStr,
            menuItems = minRatingItems.asList(),
            selectedItemIndex = (advancedOption.minRating - 1).coerceAtLeast(0),
            onSelectedItemIndexChange = {
                onAdvancedOptionChange(advancedOption.copy(minRating = if (it == 0) 0 else it + 1))
            },
        )
        val pageErr1 = stringResource(R.string.search_sp_err1)
        val pageErr2 = stringResource(R.string.search_sp_err2)
        val pages = stringResource(id = R.string.key_pages)
        val pagesText = remember(advancedOption.fromPage, advancedOption.toPage) {
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
        SearchFilterChip(
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
            label = pagesText,
        )
        fun checked(bit: Int) = advancedOption.advanceSearch and bit != 0
        fun AdvancedSearchOption.inv(bit: Int) = onAdvancedOptionChange(copy(advanceSearch = advanceSearch xor bit))
        SearchFilterChip(
            selected = checked(AdvanceTable.SH),
            onClick = { advancedOption.inv(AdvanceTable.SH) },
            label = stringResource(id = R.string.search_sh),
        )
        SearchFilterChip(
            selected = checked(AdvanceTable.STO),
            onClick = { advancedOption.inv(AdvanceTable.STO) },
            label = stringResource(id = R.string.search_sto),
        )
        val disableFilter = stringResource(id = R.string.search_sf)
        SearchFilterChip(
            selected = checked(AdvanceTable.SFL),
            onClick = { advancedOption.inv(AdvanceTable.SFL) },
            label = disableFilter + stringResource(id = R.string.search_sfl),
        )
        SearchFilterChip(
            selected = checked(AdvanceTable.SFU),
            onClick = { advancedOption.inv(AdvanceTable.SFU) },
            label = disableFilter + stringResource(id = R.string.search_sfu),
        )
        SearchFilterChip(
            selected = checked(AdvanceTable.SFT),
            onClick = { advancedOption.inv(AdvanceTable.SFT) },
            label = disableFilter + stringResource(id = R.string.search_sft),
        )
    }
}

@Composable
private fun SearchFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val backgroundColor = if (selected) {
        MiuixTheme.colorScheme.primary.copy(alpha = 0.2f)
    } else {
        MiuixTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (selected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurface
    }

    LiquidGlassSurface(
        shape = CircleShape,
        containerColor = backgroundColor,
        elevation = if (selected) 2.dp else 1.dp,
        refractionHeight = 8.dp,
        refractionAmount = 8.dp,
        modifier = modifier.clickable(role = Role.Checkbox) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = contentColor,
                style = MiuixTheme.textStyles.body2,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}
