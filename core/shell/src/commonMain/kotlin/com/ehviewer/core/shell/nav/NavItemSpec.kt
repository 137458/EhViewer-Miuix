package com.ehviewer.core.shell.nav

import androidx.compose.ui.graphics.vector.ImageVector

// 共享导航项规格：宿主各自把自身导航模型映射为 spec，壳层只消费 spec，不感知路由实现
data class NavItemSpec(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

// 底栏主项归并策略（移植自 :app MainNavPolicy）：完整导航集合里只有主项进底栏，
// 非主项（如 Toplist/History）经别名归并到主项做选中高亮
class NavPrimaryPolicy(
    val primaryKeys: List<String>,
    val mergeAliases: Map<String, String> = emptyMap(),
) {
    // 非主项归并到其别名主键；主键本身或未知 key 原样参与主项匹配
    fun resolvePrimary(key: String?): String? {
        val resolved = key?.let { mergeAliases[it] ?: it } ?: return null
        return resolved.takeIf { it in primaryKeys }
    }

    fun primaryIndexOf(key: String?): Int = primaryKeys.indexOf(resolvePrimary(key))
}
