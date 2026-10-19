package com.ehviewer.desktop

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ehviewer.core.i18n.MR
import com.ehviewer.core.shell.list.CategoryChipSpec
import dev.icerock.moko.resources.compose.stringResource

// 桌面在线搜索分类胶囊数据源：flag 与颜色和移动端 EhUtils 同值，词条走 moko 多语言资源
object DesktopCategoryChips {

    // 展示序与移动端 categoryTable 一致：DOUJINSHI..WESTERN、MISC（0x1）在表尾
    @Composable
    fun specs(): List<CategoryChipSpec> = listOf(
        CategoryChipSpec(flag = 0x2, label = stringResource(MR.strings.doujinshi), color = Color(0xFFF44336)),
        CategoryChipSpec(flag = 0x4, label = stringResource(MR.strings.manga), color = Color(0xFFFF9800)),
        CategoryChipSpec(flag = 0x8, label = stringResource(MR.strings.artist_cg), color = Color(0xFFFBC02D)),
        CategoryChipSpec(flag = 0x10, label = stringResource(MR.strings.game_cg), color = Color(0xFF4CAF50)),
        CategoryChipSpec(flag = 0x20, label = stringResource(MR.strings.image_set), color = Color(0xFF3F51B5)),
        CategoryChipSpec(flag = 0x40, label = stringResource(MR.strings.cosplay), color = Color(0xFF9C27B0)),
        CategoryChipSpec(flag = 0x80, label = stringResource(MR.strings.asian_porn), color = Color(0xFF9575CD)),
        CategoryChipSpec(flag = 0x100, label = stringResource(MR.strings.non_h), color = Color(0xFF2196F3)),
        CategoryChipSpec(flag = 0x200, label = stringResource(MR.strings.western), color = Color(0xFF8BC34A)),
        CategoryChipSpec(flag = 0x1, label = stringResource(MR.strings.misc), color = Color(0xFFF06292)),
    )
}
