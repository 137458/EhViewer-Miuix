package com.ehviewer.core.shell.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ehviewer.core.ui.component.DropdownFilterChip
import com.ehviewer.core.ui.component.LiquidGlassSurface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 下拉选择 chip 数据面（语言/最低评分一类：固定候选项 + 选中下标）
data class FilterDropdownSpec(
    val label: String,
    val items: List<String>,
    val selectedIndex: Int,
    val onSelectIndex: (Int) -> Unit,
)

// 开关 chip 数据面（高级搜索 on/off 项）
data class FilterToggleSpec(
    val label: String,
    val selected: Boolean,
    val onToggle: () -> Unit,
)

// 动作 chip 数据面（如页数区间：点击唤起宿主编辑对话框）
data class FilterActionSpec(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
)

// 库页筛选面板（移动端 SearchFilter 同构移植，数据面全部宿主注入）：
// 上半段分类胶囊组 + 下半段横向滚动 chip 行（下拉选择/开关/动作）。
// 承载于搜索展开视图头部与筛选底单两处，内部不可再嵌滚动容器（底单侧自行包 verticalScroll）。
@Composable
fun SearchFilterPanel(
    categories: List<CategoryChipSpec>,
    selectedCategory: Int,
    allCategoriesLabel: String,
    onCategoryChange: (Int) -> Unit,
    dropdowns: List<FilterDropdownSpec>,
    toggles: List<FilterToggleSpec>,
    actions: List<FilterActionSpec>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(bottom = 8.dp),
    ) {
        CategoryPillRow(
            categories = categories,
            selectedCategory = selectedCategory,
            allLabel = allCategoriesLabel,
            onCategoryChange = onCategoryChange,
        )
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            dropdowns.forEach { spec ->
                DropdownFilterChip(
                    label = spec.label,
                    menuItems = spec.items,
                    selectedItemIndex = spec.selectedIndex,
                    onSelectedItemIndexChange = spec.onSelectIndex,
                )
            }
            actions.forEach { spec ->
                FilterChipPill(selected = spec.selected, role = Role.Button, onClick = spec.onClick) {
                    Text(
                        text = spec.label,
                        color = if (spec.selected) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            MiuixTheme.colorScheme.onSurface
                        },
                        fontSize = 13.sp,
                    )
                }
            }
            toggles.forEach { spec ->
                FilterChipPill(selected = spec.selected, role = Role.Checkbox, onClick = spec.onToggle) {
                    Text(
                        text = spec.label,
                        color = if (spec.selected) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            MiuixTheme.colorScheme.onSurface
                        },
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

// 筛选 chip 胶囊（移动端 SearchFilterChip 同构）：选中 primary 半透明衬底 + 加粗，未选中 surface 衬底
@Composable
private fun FilterChipPill(
    selected: Boolean,
    role: Role,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val backgroundColor = if (selected) {
        MiuixTheme.colorScheme.primary.copy(alpha = 0.2f)
    } else {
        MiuixTheme.colorScheme.surfaceContainer
    }

    LiquidGlassSurface(
        shape = CircleShape,
        containerColor = backgroundColor,
        elevation = if (selected) 2.dp else 1.dp,
        refractionHeight = 8.dp,
        refractionAmount = 8.dp,
        modifier = Modifier.clickable(role = role) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}
