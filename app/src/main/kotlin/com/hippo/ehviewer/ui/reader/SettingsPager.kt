package com.hippo.ehviewer.ui.reader

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ehviewer.core.i18n.R
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.utils.springAnimateToPage

private val tabs = intArrayOf(
    R.string.pref_category_reading_mode,
    R.string.pref_category_general,
    R.string.custom_filter,
)

@Composable
fun SettingsPager(isWebtoon: Boolean, modifier: Modifier = Modifier, onPageSelected: (Int) -> Unit) {
    val pagerState = rememberPagerState { tabs.size }
    LaunchedEffect(onPageSelected) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onPageSelected(page)
        }
    }
    val scope = rememberCoroutineScope()
    val tabTitles = tabs.map { stringResource(id = it) }
    TabRowWithContour(
        tabs = tabTitles,
        selectedTabIndex = pagerState.currentPage,
        onTabSelected = { index ->
            scope.launch { pagerState.springAnimateToPage(index) }
        },
    )
    HorizontalPager(
        modifier = modifier,
        state = pagerState,
        verticalAlignment = Alignment.Top,
    ) { page ->
        when (page) {
            0 -> ReaderModeSetting(isWebtoon)
            1 -> ReaderGeneralSetting()
            2 -> ColorFilterSetting()
        }
    }
}
