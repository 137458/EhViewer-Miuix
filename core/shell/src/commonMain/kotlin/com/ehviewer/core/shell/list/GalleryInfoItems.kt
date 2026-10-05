package com.ehviewer.core.shell.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.ehviewer.core.database.client.thumbUrl
import com.ehviewer.core.model.BaseGalleryInfo
import com.ehviewer.core.ui.component.CrystalCard
import com.ehviewer.core.ui.component.GalleryListCardRating
import com.ehviewer.core.ui.component.SquircleShape
import com.ehviewer.core.ui.util.thenIf
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val MIN_RATIO = 0.5f
private const val MAX_RATIO = 1.5f
const val GALLERY_THUMB_DEFAULT_RATIO = 0.67f

// 移动端 CropDefaults 同规则：竖版封面（宽高比 0.5..0.8）按 Crop 填充 0.67 比例框
private const val CROP_MIN_ASPECT = 0.5f
private const val CROP_MAX_ASPECT = 0.8f
private fun shouldCrop(width: Int, height: Int) = (width.toFloat() / height) in CROP_MIN_ASPECT..CROP_MAX_ASPECT

// 封面图（与移动端 EhThumbCard 同构）：未加载成功时可点击重试，成功后按结果决定裁切
@Composable
fun GalleryThumbCard(
    key: Any,
    model: Any?,
    modifier: Modifier = Modifier,
) = Card(modifier = modifier) {
    var contentScale by remember(key) { mutableStateOf(ContentScale.Fit) }
    val painter = rememberAsyncImagePainter(
        model = model,
        onSuccess = {
            if (shouldCrop(it.result.image.width, it.result.image.height)) {
                contentScale = ContentScale.Crop
            }
        },
    )
    val state by painter.state.collectAsState()
    Image(
        painter = painter,
        contentDescription = null,
        modifier = Modifier
            .thenIf(state !is AsyncImagePainter.State.Success) {
                // 保持可点击以避免切断 ripple；失败后点击重试（移动端同语义）
                clickable { if (state is AsyncImagePainter.State.Error) painter.restart() }
            }
            .fillMaxSize(),
        contentScale = contentScale,
    )
}

// 列表行（详情卡）——移动端 GalleryInfoListItem 同构移植：
// SET/数据源等端差异经 title/status/thumbDecorate 参数注入
@Composable
fun GalleryInfoListItem(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    info: BaseGalleryInfo,
    title: String,
    modifier: Modifier = Modifier,
    status: GalleryItemStatus = GalleryItemStatus.Empty,
    showPages: Boolean = true,
    showProgress: Boolean = true,
    postedText: String? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    thumbDecorate: @Composable (content: @Composable () -> Unit) -> Unit = { it() },
) = CrystalCard(
    modifier = modifier,
    onClick = onClick,
    onLongClick = onLongClick,
    interactionSource = interactionSource,
) {
    Row {
        thumbDecorate {
            GalleryThumbCard(
                key = info.gid,
                model = info.thumbUrl,
                modifier = Modifier.aspectRatio(GALLERY_THUMB_DEFAULT_RATIO),
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
                    text = info.uploader.orEmpty(),
                    modifier = Modifier.alpha(if (info.disowned) 0.5f else 1f),
                    style = MiuixTheme.textStyles.footnote1,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (status.isInFavScene) {
                    status.favoriteNote?.let {
                        Text(
                            text = it,
                            fontStyle = FontStyle.Italic,
                            style = MiuixTheme.textStyles.footnote1,
                        )
                    }
                } else if (status.isFavorited) {
                    Icon(
                        imageVector = MiuixIcons.FavoritesFill,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp).align(Alignment.CenterVertically),
                        tint = GalleryItemVisuals.favoriteIconColor,
                    )
                    status.favoriteName?.let {
                        Text(text = it, style = MiuixTheme.textStyles.footnote1)
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GalleryListCardRating(rating = info.rating, modifier = Modifier.padding(top = 1.dp, bottom = 3.dp))
                Spacer(modifier = Modifier.weight(1f))
                if (status.isDownloaded) {
                    Icon(
                        imageVector = MiuixIcons.Download,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MiuixTheme.colorScheme.primary,
                    )
                }
                info.simpleLanguage?.let {
                    Text(text = it, style = MiuixTheme.textStyles.footnote1)
                }
                if (info.pages != 0 && showPages) {
                    val readProgress = if (showProgress) status.readProgress else 0
                    Text(
                        text = if (readProgress > 0) "${readProgress + 1}/${info.pages}P" else "${info.pages}P",
                        style = MiuixTheme.textStyles.footnote1,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryChip(info = info, status = status)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = postedText ?: info.posted.orEmpty(),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                )
            }
        }
    }
}

// 网格卡（瀑布流缩略图）——移动端 GalleryInfoGridItem 同构移植
@Composable
fun GalleryInfoGridItem(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    info: BaseGalleryInfo,
    modifier: Modifier = Modifier,
    status: GalleryItemStatus = GalleryItemStatus.Empty,
    showLanguage: Boolean = true,
    showPages: Boolean = true,
    showProgress: Boolean = true,
    showFavoriteStatus: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    thumbDecorate: @Composable (content: @Composable () -> Unit) -> Unit = { it() },
) = CrystalCard(
    modifier = modifier,
    onClick = onClick,
    onLongClick = onLongClick,
    interactionSource = interactionSource,
) {
    Box {
        thumbDecorate {
            Box {
                var ratio by remember(info.gid) {
                    val initial = if (info.thumbHeight != 0) {
                        (info.thumbWidth.toFloat() / info.thumbHeight).coerceIn(MIN_RATIO, MAX_RATIO)
                    } else {
                        GALLERY_THUMB_DEFAULT_RATIO
                    }
                    mutableFloatStateOf(initial)
                }
                AsyncImage(
                    model = info.thumbUrl,
                    contentDescription = null,
                    modifier = Modifier.aspectRatio(ratio),
                    onSuccess = {
                        ratio = (it.result.image.width.toFloat() / it.result.image.height).coerceIn(MIN_RATIO, MAX_RATIO)
                    },
                )
            }
        }
        CategoryBadge(
            info = info,
            status = status,
            showLanguage = showLanguage,
            showPages = showPages,
            showProgress = showProgress,
            modifier = Modifier.align(Alignment.TopEnd),
        )
        if (showFavoriteStatus && status.isFavorited) {
            Icon(
                imageVector = MiuixIcons.FavoritesFill,
                contentDescription = null,
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                tint = GalleryItemVisuals.favoriteIconColor,
            )
        }
    }
}

// 列表行底部类目 chip
@Composable
private fun CategoryChip(info: BaseGalleryInfo, status: GalleryItemStatus) {
    val categoryColor = GalleryItemVisuals.categoryColor(info.category, status.harmonizeColor)
    Text(
        text = labelOf(info.category),
        modifier = Modifier
            .clip(SquircleShape(6.dp))
            .background(categoryColor)
            .padding(vertical = 2.dp, horizontal = 6.dp),
        color = GalleryItemVisuals.categoryTextColor(categoryColor),
        style = MiuixTheme.textStyles.footnote2,
    )
}

// 网格卡右上角类目/页数徽标（对齐由调用方在 Box 作用域内传入）
@Composable
private fun CategoryBadge(
    info: BaseGalleryInfo,
    status: GalleryItemStatus,
    showLanguage: Boolean,
    showPages: Boolean,
    showProgress: Boolean,
    modifier: Modifier = Modifier,
) {
    val categoryColor = GalleryItemVisuals.categoryColor(info.category, status.harmonizeColor)
    Row(
        modifier = modifier
            .padding(4.dp)
            .clip(SquircleShape(6.dp))
            .background(categoryColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val shouldShowLanguage = showLanguage && info.simpleLanguage != null
        val textColor = GalleryItemVisuals.categoryTextColor(categoryColor)
        if (showPages && info.pages > 0) {
            val readProgress = if (showProgress) status.readProgress else 0
            Text(
                text = if (readProgress > 0) "${readProgress + 1}/${info.pages}" else "${info.pages}",
                style = MiuixTheme.textStyles.footnote2,
                color = textColor,
            )
            if (shouldShowLanguage) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
        if (shouldShowLanguage) {
            Text(
                text = info.simpleLanguage.orEmpty(),
                style = MiuixTheme.textStyles.footnote2,
                color = textColor,
            )
        }
    }
}

@Composable
private fun labelOf(category: Int): String = StringDesc.Resource(GalleryItemVisuals.categoryLabelRes(category)).localized().uppercase()
