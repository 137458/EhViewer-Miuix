package com.ehviewer.desktop

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ehviewer.core.i18n.MR
import com.ehviewer.core.model.GalleryPreview
import com.ehviewer.core.model.V1GalleryPreview
import com.ehviewer.core.model.V2GalleryPreview
import com.ehviewer.core.ui.component.SquircleShape
import dev.icerock.moko.resources.compose.stringResource
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 预览条：详情页预览横向滚动展示。
// V1 直链直接按框裁切显示；V2 雪碧图按 DesktopPreviewSprite 几何位移裁出单元格。
private const val PREVIEW_DISPLAY_HEIGHT = 140

@Composable
fun DesktopPreviewsSection(
    previewList: List<GalleryPreview>?,
    onPreviewImage: ((url: String) -> Unit)? = null,
) {
    val headerText = stringResource(MR.strings.gallery_previews)
    val loadingText = stringResource(MR.strings.desktop_reader_loading)

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Text(
            text = headerText,
            color = MiuixTheme.colorScheme.primary,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        )
        val previews = previewList
        when {
            previews == null -> Text(
                text = loadingText,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
            )
            previews.isEmpty() -> Text(
                text = stringResource(MR.strings.desktop_no_previews),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
            )
            else -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                previews.take(20).forEach { preview ->
                    PreviewCell(
                        preview = preview,
                        onPreviewImage = onPreviewImage,
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewCell(
    preview: GalleryPreview,
    onPreviewImage: ((url: String) -> Unit)? = null,
) {
    val clipboard = LocalClipboardManager.current
    val copyLinkText = stringResource(MR.strings.copy_link)
    val openInBrowserText = stringResource(MR.strings.open_in_browser)

    ContextMenuArea(
        items = {
            listOf(
                ContextMenuItem(copyLinkText) {
                    clipboard.setText(AnnotatedString(preview.url))
                },
                ContextMenuItem(openInBrowserText) {
                    DesktopBrowser.openUrl(preview.url)
                },
            )
        },
    ) {
        val hoverInteraction = remember { MutableInteractionSource() }
        val hovered by hoverInteraction.collectIsHoveredAsState()
        when (preview) {
            is V1GalleryPreview -> Box(
                modifier = Modifier
                    .clip(SquircleShape(8.dp))
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(
                        interactionSource = hoverInteraction,
                        indication = null,
                    ) {
                        if (onPreviewImage != null) {
                            onPreviewImage(preview.url)
                        } else {
                            DesktopBrowser.openUrl(preview.url)
                        }
                    }
                    .width(100.dp)
                    .height(PREVIEW_DISPLAY_HEIGHT.dp),
            ) {
                AsyncImage(
                    model = preview.url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                PreviewHoverOverlay(hovered)
            }
            is V2GalleryPreview -> {
                // 雪碧图单格：按显示高推算缩放，整图按位移裁出当前格（点击全屏预览或浏览器打开）
                val cell = DesktopPreviewSprite.cellLayout(
                    offsetX = preview.offsetX,
                    clipWidth = preview.clipWidth,
                    clipHeight = preview.clipHeight,
                    displayHeight = PREVIEW_DISPLAY_HEIGHT.toFloat(),
                ) ?: return@ContextMenuArea
                Box(
                    modifier = Modifier
                        .clip(SquircleShape(8.dp))
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable(
                            interactionSource = hoverInteraction,
                            indication = null,
                        ) {
                            if (onPreviewImage != null) {
                                onPreviewImage(preview.url)
                            } else {
                                DesktopBrowser.openUrl(preview.url)
                            }
                        }
                        .width(cell.width.dp)
                        .height(cell.height.dp),
                ) {
                    AsyncImage(
                        model = preview.url,
                        contentDescription = null,
                        contentScale = ContentScale.FillHeight,
                        modifier = Modifier
                            .height(PREVIEW_DISPLAY_HEIGHT.dp)
                            .offset(x = cell.offsetX.dp),
                    )
                    PreviewHoverOverlay(hovered)
                }
            }
        }
    }
}

// 预览缩略格悬停高亮：primary 半透明叠层，提示可点击全屏预览或浏览器打开
@Composable
private fun PreviewHoverOverlay(hovered: Boolean) {
    if (hovered) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.18f)),
        )
    }
}
