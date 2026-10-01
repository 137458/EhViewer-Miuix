package com.ehviewer.desktop

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.ehviewer.core.database.client.GalleryDetailPageLinksParser
import com.ehviewer.core.database.client.GalleryPageParser
import com.ehviewer.core.i18n.MR
import com.ehviewer.core.model.BaseGalleryInfo
import com.ehviewer.core.util.LogPriority
import com.ehviewer.core.util.logcat
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 阅读窗口：详情页链接提取 → 图片页解析 → 图片显示。
// 图片页与解析链路均走桌面网络栈（desktopGet，代理随 DesktopSettings/环境变量）。
@Composable
fun ReaderScreen(
    gallery: BaseGalleryInfo,
    onClose: () -> Unit,
) {
    var page by remember { mutableIntStateOf(1) }
    var pageLinks by remember { mutableStateOf<List<GalleryDetailPageLinksParser.PageLink>>(emptyList()) }
    var linksState by remember { mutableStateOf<String?>(null) }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var imageState by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var showJumpInput by remember { mutableStateOf(false) }
    var jumpInput by remember { mutableStateOf("") }
    val displayTitle = gallery.title?.takeIf { it.isNotBlank() }
        ?: gallery.titleJpn?.takeIf { it.isNotBlank() }
        ?: gallery.gid.toString()
    val context = LocalPlatformContext.current
    val clipboard = LocalClipboardManager.current
    val loadingLinksText = stringResource(MR.strings.desktop_reader_loading_links)
    val noLinksText = stringResource(MR.strings.desktop_reader_no_links)
    val loadFailedText = stringResource(MR.strings.desktop_reader_load_failed)
    val loadingImageText = stringResource(MR.strings.desktop_reader_loading_image)
    val loadingText = stringResource(MR.strings.desktop_reader_loading)

    LaunchedEffect(gallery.gid) {
        imageState = loadingLinksText
        runCatching {
            withContext(Dispatchers.IO) {
                val detail = desktopGet(galleryWebUrl(gallery.gid, gallery.token))
                GalleryDetailPageLinksParser.parse(detail.body)
            }
        }.onSuccess { links ->
            if (links.isEmpty()) {
                linksState = noLinksText
                imageState = null
            } else {
                pageLinks = links
                linksState = null
            }
        }.onFailure {
            linksState = "$loadFailedText: ${it.message}"
            imageState = null
        }
    }

    val currentLink = pageLinks.getOrNull(page - 1)
    LaunchedEffect(currentLink, reloadKey) {
        val link = currentLink ?: return@LaunchedEffect
        imageState = loadingImageText
        imageUrl = null
        withContext(Dispatchers.IO) {
            runCatching {
                val pageResponse = desktopGet(link.pageUrl)
                GalleryPageParser.parse(pageResponse.body)?.imageUrl
            }.getOrElse { "ERROR: ${it.message}" }
        }.let { result ->
            if (result?.startsWith("ERROR:") == true) {
                imageState = result.removePrefix("ERROR: ")
            } else {
                imageState = null
                imageUrl = result
            }
        }
    }

    // 预取下一页图片地址与本体：翻页时免等待（图片本体进 Coil 磁盘/内存缓存）
    val nextLink = pageLinks.getOrNull(page)
    LaunchedEffect(nextLink) {
        val link = nextLink ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            runCatching {
                val pageResponse = desktopGet(link.pageUrl)
                GalleryPageParser.parse(pageResponse.body)?.imageUrl
            }.getOrNull()?.let { url ->
                val request = coil3.request.ImageRequest.Builder(context).data(url).build()
                SingletonImageLoader.get(context).enqueue(request)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background)
            .onPreviewKeyEvent { event ->
                // 阅读器键盘翻页：←/→（仅 KeyDown 响应一次；跳页输入框打开时让位给文本编辑）
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionRight -> {
                            if (!showJumpInput && page < pageLinks.size) page += 1
                            !showJumpInput
                        }
                        Key.DirectionLeft -> {
                            if (!showJumpInput && page > 1) page -= 1
                            !showJumpInput
                        }
                        else -> false
                    }
                } else {
                    false
                }
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = displayTitle,
                color = MiuixTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (pageLinks.isEmpty()) "" else stringResource(MR.strings.desktop_reader_page_progress, page, pageLinks.size),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(enabled = pageLinks.isNotEmpty()) {
                        jumpInput = page.toString()
                        showJumpInput = !showJumpInput
                    },
            )
            if (showJumpInput && pageLinks.isNotEmpty()) {
                OutlinedTextField(
                    value = jumpInput,
                    onValueChange = { jumpInput = it.filter(Char::isDigit).take(6) },
                    singleLine = true,
                    modifier = Modifier
                        .width(72.dp)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                                jumpInput.toIntOrNull()?.let { target ->
                                    page = target.coerceIn(1, pageLinks.size)
                                }
                                showJumpInput = false
                                true
                            } else {
                                false
                            }
                        },
                )
            }
            Text(
                text = "✕",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(onClick = onClose),
            )
        }
        HorizontalDivider()

        // 图片手势状态：捏合缩放（1x-5x）+ 拖拽平移；双击重置；翻页自动重置
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        LaunchedEffect(page) {
            scale = 1f
            offset = Offset.Zero
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        offset = if (scale > 1f) offset + pan else Offset.Zero
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { tap ->
                            // 未缩放时点击左右 1/3 区域翻页（漫画阅读器标准交互）
                            if (scale <= 1f && size.width > 0) {
                                when {
                                    tap.x < size.width / 3f -> if (page > 1) page -= 1
                                    tap.x > size.width * 2f / 3f -> if (page < pageLinks.size) page += 1
                                }
                            }
                        },
                        onDoubleTap = {
                            scale = if (scale > 1f) 1f else 2.5f
                            offset = Offset.Zero
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            val url = imageUrl
            val copyLinkLabel = stringResource(MR.strings.copy_link)
            val openInBrowserLabel = stringResource(MR.strings.open_in_browser)
            when {
                url != null -> ContextMenuArea(
                    items = {
                        listOf(
                            ContextMenuItem(copyLinkLabel) {
                                clipboard.setText(AnnotatedString(url))
                            },
                            ContextMenuItem(openInBrowserLabel) {
                                DesktopBrowser.openUrl(url)
                            },
                        )
                    },
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = displayTitle,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            },
                        contentScale = ContentScale.Fit,
                    )
                }
                imageState != null -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable { reloadKey += 1 }
                        .padding(16.dp),
                ) {
                    Text(
                        text = imageState!!,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    Text(
                        text = stringResource(MR.strings.action_retry),
                        color = MiuixTheme.colorScheme.primary,
                    )
                }
                else -> Text(
                    text = loadingText,
                    color = Color.White.copy(alpha = 0.5f),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(MR.strings.desktop_reader_prev),
                color = if (page > 1) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(enabled = page > 1) { page -= 1 }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Box(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(MR.strings.desktop_reader_next),
                color = if (page < pageLinks.size) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(enabled = page < pageLinks.size) { page += 1 }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}
