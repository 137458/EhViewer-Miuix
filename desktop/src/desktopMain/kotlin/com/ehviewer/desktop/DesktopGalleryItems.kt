package com.ehviewer.desktop

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ehviewer.core.database.client.CATEGORY_ARTIST_CG
import com.ehviewer.core.database.client.CATEGORY_ASIAN_PORN
import com.ehviewer.core.database.client.CATEGORY_COSPLAY
import com.ehviewer.core.database.client.CATEGORY_DOUJINSHI
import com.ehviewer.core.database.client.CATEGORY_GAME_CG
import com.ehviewer.core.database.client.CATEGORY_IMAGE_SET
import com.ehviewer.core.database.client.CATEGORY_MANGA
import com.ehviewer.core.database.client.CATEGORY_MISC
import com.ehviewer.core.database.client.CATEGORY_NON_H
import com.ehviewer.core.database.client.CATEGORY_WESTERN
import com.ehviewer.core.database.client.thumbUrl
import com.ehviewer.core.model.BaseGalleryInfo
import com.ehviewer.core.ui.component.GalleryListCardRating
import com.ehviewer.core.ui.component.SquircleShape
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 桌面端画廊视觉映射。分类底色与移动端 EhUtils.getCategoryColor 同值（桌面无 harmonize 设置，不做色调融合）。
object DesktopGalleryVisuals {
    fun categoryColor(category: Int): Color = Color(
        when (category) {
            CATEGORY_DOUJINSHI -> 0xfff44336u
            CATEGORY_MANGA -> 0xffff9800u
            CATEGORY_ARTIST_CG -> 0xfffbc02du
            CATEGORY_GAME_CG -> 0xff4caf50u
            CATEGORY_WESTERN -> 0xff8bc34au
            CATEGORY_NON_H -> 0xff2196f3u
            CATEGORY_IMAGE_SET -> 0xff3f51b5u
            CATEGORY_COSPLAY -> 0xff9c27b0u
            CATEGORY_ASIAN_PORN -> 0xff9575cdu
            CATEGORY_MISC -> 0xfff06292u
            else -> 0xff000000u
        }.toInt(),
    )

    // 与移动端 getCategoryTextColor 同规则：L* > 70 的浅色 chip 配深字，深色 chip 配浅字
    @Composable
    fun categoryTextColor(chipColor: Color): Color = if ((chipColor.luminance() > LSTAR_70_LUMINANCE) == isSystemInDarkTheme()) {
        MiuixTheme.colorScheme.surface
    } else {
        MiuixTheme.colorScheme.onSurface
    }

    val favoriteIconColor = Color(0xFFFF3040)

    // L* = 70 对应的相对亮度（Y = ((70 + 16) / 116)^3）
    private const val LSTAR_70_LUMINANCE = 0.4076f
}

private const val MIN_THUMB_RATIO = 0.5f
private const val MAX_THUMB_RATIO = 1.5f
private const val DEFAULT_THUMB_RATIO = 0.67f

// 缩略图元数据缺失（0）时回退默认比例；元数据命中则钳制在 0.5~1.5（与移动端 GalleryInfoGridItem 同规则）
internal fun resolvedThumbRatio(thumbWidth: Int, thumbHeight: Int): Float = if (thumbHeight != 0) {
    (thumbWidth.toFloat() / thumbHeight).coerceIn(MIN_THUMB_RATIO, MAX_THUMB_RATIO)
} else {
    DEFAULT_THUMB_RATIO
}

// 选中/悬停容器色：多选 > 键盘选中 > 悬停（沿用主库既有三档透明度语义）
@Composable
private fun galleryCardColors(
    isMultiSelected: Boolean,
    isSelected: Boolean,
    hovered: Boolean,
) = when {
    isMultiSelected -> CardDefaults.defaultColors(color = MiuixTheme.colorScheme.primary.copy(alpha = 0.20f))
    isSelected -> CardDefaults.defaultColors(color = MiuixTheme.colorScheme.primary.copy(alpha = 0.12f))
    hovered -> CardDefaults.defaultColors(color = MiuixTheme.colorScheme.primary.copy(alpha = 0.06f))
    else -> CardDefaults.defaultColors()
}

// 桌面画廊列表项：视觉结构对齐移动端 GalleryInfoListItem（Card + 全比例封面 + 两行标题 +
// uploader/收藏行 + 评分/语言/页数行 + 分类色块 chip 行），交互保留桌面惯例（悬停高亮/键盘选中/Ctrl 多选）。
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DesktopGalleryListItem(
    gallery: BaseGalleryInfo,
    title: String,
    browseTime: String?,
    isMultiSelected: Boolean,
    isSelected: Boolean,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val hovered by interactionSource.collectIsHoveredAsState()
    Card(
        modifier = modifier
            .pointerHoverIcon(PointerIcon.Hand)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        colors = galleryCardColors(isMultiSelected, isSelected, hovered),
    ) {
        Row {
            val thumb = gallery.thumbUrl
            if (thumb != null) {
                AsyncImage(
                    model = thumb,
                    contentDescription = title,
                    modifier = Modifier
                        .aspectRatio(DEFAULT_THUMB_RATIO)
                        .clip(SquircleShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(modifier = Modifier.padding(start = 8.dp, top = 2.dp, end = 4.dp, bottom = 4.dp)) {
                Text(
                    text = title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MiuixTheme.textStyles.body1,
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = gallery.uploader.orEmpty(),
                        modifier = Modifier.alpha(if (gallery.disowned) 0.5f else 1f),
                        style = MiuixTheme.textStyles.footnote1,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (isFavorite) {
                        Icon(
                            imageVector = MiuixIcons.FavoritesFill,
                            contentDescription = null,
                            modifier = Modifier
                                .size(14.dp)
                                .align(Alignment.CenterVertically),
                            tint = DesktopGalleryVisuals.favoriteIconColor,
                        )
                        gallery.favoriteName?.let {
                            Text(text = it, style = MiuixTheme.textStyles.footnote1)
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GalleryListCardRating(
                        rating = gallery.rating,
                        modifier = Modifier.padding(top = 1.dp, bottom = 3.dp),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (isDownloaded) {
                        Icon(
                            imageVector = MiuixIcons.Download,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MiuixTheme.colorScheme.primary,
                        )
                    }
                    gallery.simpleLanguage?.let {
                        Text(text = it, style = MiuixTheme.textStyles.footnote1)
                    }
                    if (gallery.pages != 0) {
                        Text(text = "${gallery.pages}P", style = MiuixTheme.textStyles.footnote1)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val categoryColor = DesktopGalleryVisuals.categoryColor(gallery.category)
                    Text(
                        text = DesktopCategories.displayName(gallery.category).uppercase(),
                        modifier = Modifier
                            .clip(SquircleShape(6.dp))
                            .background(categoryColor)
                            .padding(vertical = 2.dp, horizontal = 6.dp),
                        color = DesktopGalleryVisuals.categoryTextColor(categoryColor),
                        style = MiuixTheme.textStyles.footnote2,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = browseTime ?: gallery.posted.orEmpty(),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
    }
}

// 桌面画廊网格项：对齐移动端 GalleryInfoGridItem（纯封面卡 + 分类/语言角标 + 收藏角标），
// 封面比例自适应（0.5~1.5 钳制，加载成功后按真实图幅修正）。
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DesktopGalleryGridItem(
    gallery: BaseGalleryInfo,
    title: String,
    isMultiSelected: Boolean,
    isSelected: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val hovered by interactionSource.collectIsHoveredAsState()
    Card(
        modifier = modifier
            .pointerHoverIcon(PointerIcon.Hand)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        colors = galleryCardColors(isMultiSelected, isSelected, hovered),
    ) {
        Box {
            var ratio by remember(gallery) { mutableFloatStateOf(resolvedThumbRatio(gallery.thumbWidth, gallery.thumbHeight)) }
            val thumb = gallery.thumbUrl
            if (thumb != null) {
                AsyncImage(
                    model = thumb,
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio)
                        .clip(SquircleShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    onSuccess = { result ->
                        ratio = (result.result.image.width.toFloat() / result.result.image.height)
                            .coerceIn(MIN_THUMB_RATIO, MAX_THUMB_RATIO)
                    },
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio)
                        .clip(SquircleShape(8.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainerHighest),
                )
            }
            val categoryColor = DesktopGalleryVisuals.categoryColor(gallery.category)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .clip(SquircleShape(6.dp))
                    .background(categoryColor)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val textColor = DesktopGalleryVisuals.categoryTextColor(categoryColor)
                if (gallery.pages > 0) {
                    Text(text = "${gallery.pages}", style = MiuixTheme.textStyles.footnote2, color = textColor)
                }
                gallery.simpleLanguage?.let {
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(text = it, style = MiuixTheme.textStyles.footnote2, color = textColor)
                }
            }
            if (isFavorite) {
                Icon(
                    imageVector = MiuixIcons.FavoritesFill,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp),
                    tint = DesktopGalleryVisuals.favoriteIconColor,
                )
            }
        }
    }
}
