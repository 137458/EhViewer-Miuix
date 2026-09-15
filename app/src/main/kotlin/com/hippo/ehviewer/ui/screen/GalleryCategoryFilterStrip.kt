package com.hippo.ehviewer.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ehviewer.core.i18n.R
import com.ehviewer.core.ui.component.LiquidGlassSurface
import com.ehviewer.core.ui.component.SquircleShape
import com.hippo.ehviewer.client.EhUtils
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val CATEGORY_ITEMS = listOf(
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
fun GalleryCategoryFilterStrip(
    selectedCategory: Int,
    onSelectCategory: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val isAllSelected = selectedCategory == EhUtils.ALL_CATEGORY || selectedCategory == EhUtils.NONE || selectedCategory <= 0

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // "全部" 胶囊
        item(key = "all") {
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
                    onSelectCategory(EhUtils.ALL_CATEGORY)
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
        }

        // 分类胶囊列表
        items(CATEGORY_ITEMS, key = { it.first }) { (cat, stringRes) ->
            val isSelected = !isAllSelected && (selectedCategory and cat != 0)
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
                        val newCat = selectedCategory and cat.inv()
                        onSelectCategory(if (newCat == 0) EhUtils.ALL_CATEGORY else newCat)
                    } else {
                        if (isAllSelected) {
                            onSelectCategory(cat)
                        } else {
                            onSelectCategory(selectedCategory or cat)
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
}
