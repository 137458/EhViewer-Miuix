package com.hippo.ehviewer.ui.main

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ehviewer.core.i18n.R
import com.ehviewer.core.model.GalleryDetail
import com.ehviewer.core.model.GalleryInfo
import com.ehviewer.core.ui.component.SquircleShape
import com.ehviewer.core.ui.icons.EhIcons
import com.ehviewer.core.ui.icons.big.SadAndroid
import com.ehviewer.core.ui.util.TransitionsVisibilityScope
import com.ehviewer.core.ui.util.detailThumbGenerator
import com.hippo.ehviewer.client.EhUtils
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Blocklist
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GalleryDetailHeaderInfoCard(
    detail: GalleryDetail,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = detail.run {
    Card(
        onClick = onClick,
        modifier = modifier.width(IntrinsicSize.Max),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row {
                Text(
                    text = language.orEmpty(),
                    style = MiuixTheme.textStyles.footnote1,
                )
                Spacer(modifier = Modifier.width(16.dp).weight(1f))
                Text(
                    text = size.orEmpty(),
                    style = MiuixTheme.textStyles.footnote1,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Text(
                    text = stringResource(id = R.string.favored_times, favoriteCount),
                    modifier = Modifier.alignByBaseline(),
                    style = MiuixTheme.textStyles.footnote1,
                )
                Spacer(modifier = Modifier.width(16.dp).weight(1f))
                Text(
                    text = pluralStringResource(id = R.plurals.page_count, pages, pages),
                    modifier = Modifier.alignByBaseline(),
                    style = MiuixTheme.textStyles.footnote1,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = posted.orEmpty(),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceSecondary,
            )
        }
    }
}

@Composable
context(_: SharedTransitionScope, _: TransitionsVisibilityScope)
fun GalleryDetailHeaderCard(
    info: GalleryInfo,
    onInfoCardClick: () -> Unit,
    onUploaderChipClick: () -> Unit,
    onBlockUploaderIconClick: () -> Unit,
    onCategoryChipClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Card(modifier = modifier) {
    Row {
        with(detailThumbGenerator) {
            EhThumbCard(
                key = remember(info.gid) { info },
                modifier = Modifier.size(
                    dimensionResource(id = com.hippo.ehviewer.R.dimen.gallery_detail_thumb_width),
                    dimensionResource(id = com.hippo.ehviewer.R.dimen.gallery_detail_thumb_height),
                ),
            )
        }
        Spacer(modifier = Modifier.weight(0.5F))
        Column(
            modifier = Modifier.height(dimensionResource(id = com.hippo.ehviewer.R.dimen.gallery_detail_thumb_height)),
            horizontalAlignment = Alignment.End,
        ) {
            (info as? GalleryDetail)?.let {
                GalleryDetailHeaderInfoCard(
                    detail = it,
                    onClick = onInfoCardClick,
                    modifier = Modifier.padding(top = 8.dp, end = dimensionResource(id = com.hippo.ehviewer.R.dimen.keyline_margin)),
                )
            }
            Spacer(modifier = Modifier.weight(1F))
            val categoryText = EhUtils.getCategory(info.category).uppercase()
            Row(
                modifier = Modifier
                    .padding(horizontal = dimensionResource(id = com.hippo.ehviewer.R.dimen.keyline_margin), vertical = 3.dp)
                    .clip(SquircleShape(8.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainer)
                    .clickable(onClick = onCategoryChipClick)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = MiuixIcons.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MiuixTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = categoryText,
                    overflow = TextOverflow.Visible,
                    softWrap = false,
                    maxLines = 1,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.primary,
                )
            }
            info.uploader?.takeIf { it.isNotEmpty() }?.let { uploaderText ->
                val canBlock = uploaderText != "(Disowned)"
                Row(
                    modifier = Modifier
                        .padding(horizontal = dimensionResource(id = com.hippo.ehviewer.R.dimen.keyline_margin), vertical = 3.dp)
                        .clip(SquircleShape(8.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainer),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (canBlock) {
                        IconButton(
                            onClick = onBlockUploaderIconClick,
                            modifier = Modifier.size(28.dp),
                            cornerRadius = 8.dp,
                            minHeight = 28.dp,
                            minWidth = 28.dp,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Blocklist,
                                contentDescription = stringResource(id = R.string.block_uploader),
                                modifier = Modifier.size(16.dp),
                                tint = MiuixTheme.colorScheme.onSurfaceSecondary,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clickable(onClick = onUploaderChipClick)
                            .padding(
                                start = if (canBlock) 2.dp else 10.dp,
                                end = 10.dp,
                                top = 6.dp,
                                bottom = 6.dp,
                            ),
                    ) {
                        Text(
                            text = uploaderText,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false,
                            maxLines = 1,
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GalleryDetailErrorTip(error: String, onClick: () -> Unit) = Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
) {
    Icon(
        imageVector = EhIcons.Big.Default.SadAndroid,
        contentDescription = stringResource(id = R.string.action_retry),
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
        tint = MiuixTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.size(8.dp))
    Text(
        text = error,
        modifier = Modifier.widthIn(max = 228.dp),
        style = MiuixTheme.textStyles.body2,
        color = MiuixTheme.colorScheme.onSurfaceSecondary,
    )
}
