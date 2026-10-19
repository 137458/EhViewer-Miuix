package com.ehviewer.core.shell.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ehviewer.core.ui.component.LiquidGlassSurface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 分类胶囊数据面（宿主注入 flag/显示名/类目色；共享层不感知 EH 具体分类表）
data class CategoryChipSpec(
    val flag: Int,
    val label: String,
    val color: Color,
)

// 分类位掩码（与移动端 EhUtils.ALL_CATEGORY 同值）；「全部」态在 UI 层仅作高亮判定
object CategoryChipFlags {
    const val ALL = 0x3FF
}

// 库页分类条（移动端 GalleryCategoryFilterStrip 同构移植）：折叠态顶栏下的横向胶囊快切条。
// 选中语义与移动端一致：全类=仅高亮「全部」；已选类点击=取消该类（清空则回全类）；全类态点击=单选该类。
@Composable
fun CategoryFilterStrip(
    categories: List<CategoryChipSpec>,
    selectedCategory: Int,
    allLabel: String,
    onSelectCategory: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "all") {
            CategoryAllPill(
                selectedCategory = selectedCategory,
                allLabel = allLabel,
                onSelectCategory = onSelectCategory,
            )
        }
        items(categories, key = { it.flag }) { spec ->
            CategoryPill(
                spec = spec,
                selected = selectedCategory > 0 && selectedCategory and spec.flag != 0,
                isAllSelected = selectedCategory == CategoryChipFlags.ALL || selectedCategory <= 0,
                currentSelection = selectedCategory,
                onSelectCategory = onSelectCategory,
            )
        }
    }
}

// 筛选面板里的分类胶囊组（移动端 SearchFilter 上半段同构）：与分类条同一交互语义，纵向换行排布
@Composable
fun CategoryPillRow(
    categories: List<CategoryChipSpec>,
    selectedCategory: Int,
    allLabel: String,
    onCategoryChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CategoryAllPill(
            selectedCategory = selectedCategory,
            allLabel = allLabel,
            onSelectCategory = onCategoryChange,
        )
        categories.forEach { spec ->
            CategoryPill(
                spec = spec,
                selected = selectedCategory > 0 && selectedCategory and spec.flag != 0,
                isAllSelected = selectedCategory == CategoryChipFlags.ALL || selectedCategory <= 0,
                currentSelection = selectedCategory,
                onSelectCategory = onCategoryChange,
            )
        }
    }
}

@Composable
private fun CategoryAllPill(
    selectedCategory: Int,
    allLabel: String,
    onSelectCategory: (Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val isAllSelected = selectedCategory == CategoryChipFlags.ALL || selectedCategory <= 0
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
            onSelectCategory(CategoryChipFlags.ALL)
        },
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            CategoryPillText(text = allLabel, selected = isAllSelected, color = allText)
        }
    }
}

@Composable
private fun CategoryPill(
    spec: CategoryChipSpec,
    selected: Boolean,
    isAllSelected: Boolean,
    currentSelection: Int,
    onSelectCategory: (Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val pillBg = if (selected) spec.color else MiuixTheme.colorScheme.surfaceContainer
    val pillText = if (selected) Color.White else MiuixTheme.colorScheme.onSurface
    LiquidGlassSurface(
        shape = CircleShape,
        containerColor = pillBg,
        elevation = if (selected) 3.dp else 1.dp,
        refractionHeight = 8.dp,
        refractionAmount = 8.dp,
        modifier = Modifier.clickable(role = Role.RadioButton) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (selected) {
                val newCat = currentSelection and spec.flag.inv()
                onSelectCategory(if (newCat == 0) CategoryChipFlags.ALL else newCat)
            } else if (isAllSelected) {
                onSelectCategory(spec.flag)
            } else {
                onSelectCategory(currentSelection or spec.flag)
            }
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!selected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(spec.color),
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            CategoryPillText(text = spec.label, selected = selected, color = pillText)
        }
    }
}

@Composable
private fun CategoryPillText(text: String, selected: Boolean, color: Color) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = color,
    )
}
