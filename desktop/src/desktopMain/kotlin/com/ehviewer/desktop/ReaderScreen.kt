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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.ehviewer.core.database.client.GalleryDetailPageLinksParser
import com.ehviewer.core.database.client.GalleryPageParser
import com.ehviewer.core.i18n.MR
import com.ehviewer.core.model.BaseGalleryInfo
import com.ehviewer.core.ui.component.SquircleShape
import com.ehviewer.core.util.LogPriority
import com.ehviewer.core.util.logcat
import dev.icerock.moko.resources.compose.stringResource
import dev.icerock.moko.resources.desc.ResourceFormatted
import dev.icerock.moko.resources.desc.StringDesc
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 阅读窗口：详情页链接提取 → 图片页解析 → 图片显示。
// 图片页与解析链路均走桌面网络栈（desktopGet，代理随 DesktopSettings/环境变量）。
@Composable
fun ReaderScreen(
    gallery: BaseGalleryInfo,
    onClose: () -> Unit,
) {
    var page by remember { mutableIntStateOf(1) }
    // 缩放/平移统一状态模型（DesktopReaderZoomState）：手势、双击、键盘缩放/平移、翻页重置共用
    // 同一边界（1x-5x）与平移不变量（offset 恒在视口钳制内，原尺寸即零平移）
    var zoomState by remember { mutableStateOf(DesktopReaderZoomState()) }
    val scrollPager = remember { DesktopScrollPager() }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var pageLinks by remember { mutableStateOf<List<GalleryDetailPageLinksParser.PageLink>>(emptyList()) }
    var linksState by remember { mutableStateOf<String?>(null) }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var imageState by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var showJumpInput by remember { mutableStateOf(false) }
    val jumpFieldState = rememberTextFieldState()
    val jumpInput = jumpFieldState.text.toString()
    // 轻量通知（保存图片等操作反馈），2.5s 自动过期
    var notifications by remember { mutableStateOf<List<DesktopNotification>>(emptyList()) }
    val nextNotificationId = remember { AtomicLong(1L) }
    val displayTitle = gallery.title?.takeIf { it.isNotBlank() }
        ?: gallery.titleJpn?.takeIf { it.isNotBlank() }
        ?: gallery.gid.toString()
    val context = LocalPlatformContext.current
    // 阅读方向（RTL 日漫：右区上一页、← 为下一页）
    val readingDirection = DesktopReadingDirection.fromPersisted(DesktopSettings.readingDirection.value)
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
                // 错误页 HTML 不进解析器：非 2xx 直接走本地化失败通道（与主库列表链路同约定）
                if (detail.status !in 200..299) error("HTTP ${detail.status}")
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
            linksState = it.message
                ?.let { message -> StringDesc.ResourceFormatted(MR.strings.desktop_reader_load_failed_reason, message).localized() }
                ?: loadFailedText
            imageState = null
        }
    }

    // 翻页自动重置缩放与平移
    LaunchedEffect(page) {
        zoomState = DesktopReaderZoomState()
    }
    AutoExpireNotifications(notifications) { notifications = it }
    val currentLink = pageLinks.getOrNull(page - 1)
    LaunchedEffect(currentLink, reloadKey) {
        val link = currentLink ?: return@LaunchedEffect
        imageState = loadingImageText
        imageUrl = null
        val outcome = withContext(Dispatchers.IO) {
            runCatching {
                val pageResponse = desktopGet(link.pageUrl)
                if (pageResponse.status !in 200..299) error("HTTP ${pageResponse.status}")
                GalleryPageParser.parse(pageResponse.body)?.imageUrl
            }
        }
        outcome.fold(
            onSuccess = { url ->
                imageState = null
                imageUrl = url
            },
            onFailure = { cause ->
                // 解析/网络异常经本地化模板展示，原始异常文本作为占位参数
                imageState = cause.message
                    ?.let { message -> StringDesc.ResourceFormatted(MR.strings.desktop_reader_load_failed_reason, message).localized() }
                    ?: loadFailedText
            },
        )
    }

    // 预取下一页图片地址与本体：翻页时免等待（图片本体进 Coil 磁盘/内存缓存）
    val nextLink = pageLinks.getOrNull(page)
    LaunchedEffect(nextLink) {
        val link = nextLink ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            runCatching {
                val pageResponse = desktopGet(link.pageUrl)
                if (pageResponse.status !in 200..299) error("HTTP ${pageResponse.status}")
                GalleryPageParser.parse(pageResponse.body)?.imageUrl
            }.getOrNull()?.let { url ->
                val request = coil3.request.ImageRequest.Builder(context).data(url).build()
                SingletonImageLoader.get(context).enqueue(request)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background)
                .onPreviewKeyEvent { event ->
                    // 阅读器键盘翻页：←/→ 与 PageUp/PageDown/Space 相对翻页（随阅读方向反转）、Home/End 恒跳首/末页
                    // （仅 KeyDown 响应一次；跳页输入框打开时让位给文本编辑）
                    if (event.type == KeyEventType.KeyDown) {
                        // 跳页输入打开时 Esc 先关闭输入框，避免误触直接关掉整个阅读窗口
                        if (event.key == Key.Escape && showJumpInput) {
                            showJumpInput = false
                            return@onPreviewKeyEvent true
                        }
                        // 缩放态方向键优先平移图片（未缩放返回 null 归翻页语义；跳页输入打开时让位）
                        if (zoomState.scale > READER_MIN_SCALE && !showJumpInput) {
                            zoomState.panned(event.key, viewportSize)?.let {
                                zoomState = it
                                return@onPreviewKeyEvent true
                            }
                        }
                        when (event.key) {
                            // F5/Ctrl+R 重载当前页（加载失败重试同通道）
                            Key.F5 -> {
                                reloadKey += 1
                                true
                            }
                            Key.R -> if (event.isCtrlPressed) {
                                reloadKey += 1
                                true
                            } else {
                                false
                            }
                            Key.DirectionRight -> {
                                val delta = readingDirection.pageDeltaForKey(forward = true)
                                if (!showJumpInput && page + delta in 1..pageLinks.size) page += delta
                                !showJumpInput
                            }
                            Key.DirectionLeft -> {
                                val delta = readingDirection.pageDeltaForKey(forward = false)
                                if (!showJumpInput && page + delta in 1..pageLinks.size) page += delta
                                !showJumpInput
                            }
                            else -> {
                                val zoomAction = resolveReaderZoom(
                                    isKeyDown = event.type == KeyEventType.KeyDown,
                                    isCtrlPressed = event.isCtrlPressed,
                                    key = event.key,
                                )
                                if (zoomAction != null) {
                                    zoomState = zoomState.keyboardZoom(zoomAction, viewportSize)
                                    true
                                } else {
                                    val nav = resolveReaderNav(event.key)
                                    if (nav != null && !showJumpInput && pageLinks.isNotEmpty()) {
                                        when (nav) {
                                            DesktopReaderNav.FirstPage -> page = 1
                                            DesktopReaderNav.LastPage -> page = pageLinks.size
                                            DesktopReaderNav.RelativeForward,
                                            DesktopReaderNav.RelativeBackward,
                                            -> {
                                                val delta = readingDirection.pageDeltaForNav(nav)
                                                if (page + delta in 1..pageLinks.size) page += delta
                                            }
                                        }
                                        true
                                    } else {
                                        false
                                    }
                                }
                            }
                        }
                    } else {
                        false
                    }
                },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MiuixTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = displayTitle,
                    color = MiuixTheme.colorScheme.primary,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (pageLinks.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(SquircleShape(6.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable {
                                jumpFieldState.setTextAndPlaceCursorAtEnd(page.toString())
                                showJumpInput = !showJumpInput
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = stringResource(MR.strings.desktop_reader_page_progress, page, pageLinks.size),
                            color = MiuixTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                        )
                    }
                }
                if (showJumpInput && pageLinks.isNotEmpty()) {
                    val jumpFocusRequester = remember { FocusRequester() }
                    LaunchedEffect(Unit) {
                        jumpFocusRequester.requestFocus()
                    }
                    val executeJump = {
                        jumpInput.toIntOrNull()?.let { target ->
                            page = target.coerceIn(1, pageLinks.size)
                        }
                        showJumpInput = false
                    }
                    Row(
                        modifier = Modifier
                            .clip(SquircleShape(8.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                            .padding(start = 6.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        TextField(
                            state = jumpFieldState,
                            lineLimits = TextFieldLineLimits.SingleLine,
                            modifier = Modifier
                                .width(56.dp)
                                .focusRequester(jumpFocusRequester)
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown) {
                                        when (event.key) {
                                            Key.Enter, Key.NumPadEnter -> {
                                                executeJump()
                                                true
                                            }
                                            Key.Escape -> {
                                                showJumpInput = false
                                                true
                                            }
                                            else -> false
                                        }
                                    } else {
                                        false
                                    }
                                },
                        )
                        Box(
                            modifier = Modifier
                                .clip(SquircleShape(6.dp))
                                .background(MiuixTheme.colorScheme.primary)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable { executeJump() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = stringResource(MR.strings.go_to),
                                color = MiuixTheme.colorScheme.onPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(SquircleShape(6.dp))
                                .background(MiuixTheme.colorScheme.surfaceContainer)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable { showJumpInput = false }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = stringResource(MR.strings.desktop_action_cancel),
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .pointerHoverIcon(PointerIcon.Hand),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
            HorizontalDivider()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
                    .onSizeChanged { viewportSize = it }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            zoomState = zoomState.gestureZoom(zoomFactor = zoom, pan = pan, viewport = size)
                        }
                    }
                    .pointerInput(Unit) {
                        // 滚轮翻页（未缩放时）：小步累计越阈翻一页，向下滚 = 下一页；缩放态让位给拖拽平移
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val scrollY = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                if (zoomState.scale <= READER_MIN_SCALE && scrollY != 0f && pageLinks.isNotEmpty()) {
                                    scrollPager.onDelta(scrollY)?.let { delta ->
                                        val target = page + delta
                                        if (target in 1..pageLinks.size) page = target
                                    }
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { tap ->
                                // 未缩放时点击左右 1/3 区域翻页（方向随阅读方向设置）
                                if (zoomState.scale <= READER_MIN_SCALE && size.width > 0) {
                                    val rightZone = tap.x > size.width / 2f
                                    val delta = readingDirection.pageDeltaForZone(rightZone = rightZone)
                                    val target = page + delta
                                    if (target in 1..pageLinks.size) page = target
                                }
                            },
                            onDoubleTap = {
                                zoomState = zoomState.doubleTapToggled()
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                // 缩放角标：scale 经 derivedStateOf 延迟读取，捏合期间仅角标自身重组（Box 内容其余部分不参与）
                ZoomBadge(
                    zoomStateState = remember { derivedStateOf { zoomState } },
                    onReset = { zoomState = DesktopReaderZoomState() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                )
                val url = imageUrl
                val copyLinkLabel = stringResource(MR.strings.copy_link)
                val openInBrowserLabel = stringResource(MR.strings.open_in_browser)
                val saveLabel = stringResource(MR.strings.action_save)
                val saveFailedText = stringResource(MR.strings.error_cant_save_image)
                val prevPageLabel = stringResource(MR.strings.desktop_reader_prev)
                val nextPageLabel = stringResource(MR.strings.desktop_reader_next)
                val retryLabel = stringResource(MR.strings.action_retry)
                val coroutineScope = rememberCoroutineScope()
                when {
                    url != null -> ContextMenuArea(
                        items = {
                            buildList {
                                if (page > 1) {
                                    add(ContextMenuItem(prevPageLabel) { page -= 1 })
                                }
                                if (page < pageLinks.size) {
                                    add(ContextMenuItem(nextPageLabel) { page += 1 })
                                }
                                add(ContextMenuItem(retryLabel) { reloadKey += 1 })
                                add(
                                    ContextMenuItem(copyLinkLabel) {
                                        clipboard.setText(AnnotatedString(url))
                                    },
                                )
                                add(
                                    ContextMenuItem(openInBrowserLabel) {
                                        DesktopBrowser.openUrl(url)
                                    },
                                )
                                add(
                                    ContextMenuItem(saveLabel) {
                                        coroutineScope.launch {
                                            runCatching {
                                                DesktopImageSaver.saveImage(url, gallery.gid, page)
                                            }.onSuccess { target ->
                                                // image_saved 模板含 %s 路径占位；点击回调非组合语境，经 StringDesc 本地化
                                                notifications = DesktopNotificationManager.post(
                                                    current = notifications,
                                                    message = StringDesc.ResourceFormatted(MR.strings.image_saved, target.toString()).localized(),
                                                    timestamp = System.currentTimeMillis(),
                                                    idProvider = { nextNotificationId.getAndIncrement() },
                                                )
                                            }.onFailure { cause ->
                                                val detail = cause.message
                                                notifications = DesktopNotificationManager.post(
                                                    current = notifications,
                                                    message = if (detail == null) {
                                                        saveFailedText
                                                    } else {
                                                        StringDesc.ResourceFormatted(MR.strings.desktop_save_failed_reason, detail).localized()
                                                    },
                                                    timestamp = System.currentTimeMillis(),
                                                    idProvider = { nextNotificationId.getAndIncrement() },
                                                )
                                            }
                                        }
                                    },
                                )
                            }
                        },
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = displayTitle,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = zoomState.scale
                                    scaleY = zoomState.scale
                                    translationX = zoomState.offset.x
                                    translationY = zoomState.offset.y
                                },
                            contentScale = ContentScale.Fit,
                        )
                    }
                    imageState != null -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Text(
                            text = imageState!!,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                        )
                        Box(
                            modifier = Modifier
                                .clip(SquircleShape(8.dp))
                                .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable { reloadKey += 1 }
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(MR.strings.action_retry),
                                color = MiuixTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    else -> Text(
                        text = loadingText,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                }
            }

            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MiuixTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(SquircleShape(8.dp))
                        .background(if (page > 1) MiuixTheme.colorScheme.surfaceContainerHighest else Color.Transparent)
                        .then(if (page > 1) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                        .clickable(enabled = page > 1) { page -= 1 }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = stringResource(MR.strings.desktop_reader_prev),
                        color = if (page > 1) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                    )
                }
                if (pageLinks.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(SquircleShape(6.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable {
                                jumpFieldState.setTextAndPlaceCursorAtEnd(page.toString())
                                showJumpInput = !showJumpInput
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "$page / ${pageLinks.size}",
                            color = MiuixTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(SquircleShape(8.dp))
                        .background(if (page < pageLinks.size) MiuixTheme.colorScheme.surfaceContainerHighest else Color.Transparent)
                        .then(if (page < pageLinks.size) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                        .clickable(enabled = page < pageLinks.size) { page += 1 }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = stringResource(MR.strings.desktop_reader_next),
                        color = if (page < pageLinks.size) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                    )
                }
            }
        }
        if (notifications.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End,
            ) {
                notifications.forEach { notice ->
                    Box(
                        modifier = Modifier
                            .clip(SquircleShape(8.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable {
                                notifications = DesktopNotificationManager.dismiss(notifications, notice.id)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = notice.message,
                                color = MiuixTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (notice.actionLabel != null) {
                                Text(
                                    text = notice.actionLabel,
                                    color = MiuixTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .clickable {
                                            notice.onAction?.invoke()
                                            notifications = DesktopNotificationManager.dismiss(notifications, notice.id)
                                        },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomBadge(
    zoomStateState: State<DesktopReaderZoomState>,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible by remember { derivedStateOf { zoomStateState.value.scale != READER_MIN_SCALE } }
    if (!visible) return
    val text by remember { derivedStateOf { DesktopZoomController.formatZoomPercentage(zoomStateState.value.scale) } }
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.85f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f), SquircleShape(6.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable { onReset() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
