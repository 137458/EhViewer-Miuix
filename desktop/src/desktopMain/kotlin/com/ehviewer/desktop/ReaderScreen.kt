package com.ehviewer.desktop

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextDecoration
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
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
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
    // imageState 同时承载加载中与失败文案，error 标记区分二者以应用语义色
    var imageStateIsError by remember { mutableStateOf(false) }
    // linksState 承载无链接提示（中性）与链接解析失败（error），error 标记区分并决定是否提供重试
    var linksStateIsError by remember { mutableStateOf(false) }
    // 链接加载重试通道：与图片重载（reloadKey）分离，链接失败态的重试/F5 走此键才会重新解析画廊页
    var linksReloadKey by remember { mutableIntStateOf(0) }
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
    val readingDirectionRaw by DesktopSettings.readingDirection.valueFlow().collectAsState(DesktopSettings.readingDirection.value)
    val readingDirection = DesktopReadingDirection.fromPersisted(readingDirectionRaw)
    // RTL 阅读推进向左：上一页为 page+1、下一页为 page-1，边界与箭头随方向镜像（增量复用已测 pageDeltaForNav）
    val isRtl = readingDirection == DesktopReadingDirection.RTL
    val prevDelta = readingDirection.pageDeltaForNav(DesktopReaderNav.RelativeBackward)
    val nextDelta = readingDirection.pageDeltaForNav(DesktopReaderNav.RelativeForward)
    val canPrev = canNavigateByDelta(page, prevDelta, pageLinks.size)
    val canNext = canNavigateByDelta(page, nextDelta, pageLinks.size)
    val prevPageLabel = stringResource(if (isRtl) MR.strings.desktop_reader_prev_rtl else MR.strings.desktop_reader_prev)
    val nextPageLabel = stringResource(if (isRtl) MR.strings.desktop_reader_next_rtl else MR.strings.desktop_reader_next)
    val clipboard = LocalClipboardManager.current
    val loadingLinksText = stringResource(MR.strings.desktop_reader_loading_links)
    val noLinksText = stringResource(MR.strings.desktop_reader_no_links)
    val loadFailedText = stringResource(MR.strings.desktop_reader_load_failed)
    val loadingImageText = stringResource(MR.strings.desktop_reader_loading_image)
    val loadingText = stringResource(MR.strings.desktop_reader_loading)

    LaunchedEffect(gallery.gid, linksReloadKey) {
        imageState = loadingLinksText
        imageStateIsError = false
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
                linksStateIsError = false
                imageState = null
            } else {
                pageLinks = links
                linksState = null
                linksStateIsError = false
                // 跨会话阅读进度：从持久化 "gid:page" 记录恢复上次读到的一页（越界钳制）
                val savedPage = DesktopReadingProgress.decode(DesktopSettings.readingProgress.value)[gallery.gid] ?: 1
                page = savedPage.coerceIn(1, links.size)
            }
        }.onFailure {
            linksState = it.message
                ?.let { message -> StringDesc.ResourceFormatted(MR.strings.desktop_reader_load_failed_reason, message).localized() }
                ?: loadFailedText
            linksStateIsError = true
            imageState = null
        }
    }

    // 翻页自动重置缩放与平移，并写入跨会话阅读进度（"gid:page" 编解码经 DesktopReadingProgress）
    LaunchedEffect(page) {
        zoomState = DesktopReaderZoomState()
        DesktopSettings.readingProgress.value = DesktopReadingProgress.encode(
            DesktopReadingProgress.update(
                progress = DesktopReadingProgress.decode(DesktopSettings.readingProgress.value),
                gid = gallery.gid,
                page = page,
            ),
        )
    }
    AutoExpireNotifications(notifications) { notifications = it }
    val currentLink = pageLinks.getOrNull(page - 1)
    LaunchedEffect(currentLink, reloadKey) {
        val link = currentLink ?: return@LaunchedEffect
        imageState = loadingImageText
        imageStateIsError = false
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
                imageStateIsError = true
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
                            // F5/Ctrl+R 重载当前页（加载失败重试同通道）；链接解析失败态走链接重试通道
                            Key.F5 -> {
                                if (linksStateIsError) linksReloadKey += 1 else reloadKey += 1
                                true
                            }
                            Key.R -> if (event.isCtrlPressed) {
                                if (linksStateIsError) linksReloadKey += 1 else reloadKey += 1
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
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .pointerHoverIcon(PointerIcon.Hand),
                ) {
                    Icon(
                        imageVector = MiuixIcons.Back,
                        contentDescription = stringResource(MR.strings.desktop_a11y_back),
                        tint = MiuixTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = displayTitle,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (pageLinks.isNotEmpty()) {
                    val topPageHover = remember { MutableInteractionSource() }
                    val topPageHovered by topPageHover.collectIsHoveredAsState()
                    Box(
                        modifier = Modifier
                            .clip(SquircleShape(6.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                            .background(if (topPageHovered) MiuixTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                            .hoverable(topPageHover)
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
                        val jumpGoHover = remember { MutableInteractionSource() }
                        val jumpGoHovered by jumpGoHover.collectIsHoveredAsState()
                        Box(
                            modifier = Modifier
                                .clip(SquircleShape(6.dp))
                                .background(MiuixTheme.colorScheme.primary)
                                .background(if (jumpGoHovered) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                                .hoverable(jumpGoHover)
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
                        val jumpCancelHover = remember { MutableInteractionSource() }
                        val jumpCancelHovered by jumpCancelHover.collectIsHoveredAsState()
                        Box(
                            modifier = Modifier
                                .clip(SquircleShape(6.dp))
                                .background(MiuixTheme.colorScheme.surfaceContainer)
                                .background(if (jumpCancelHovered) MiuixTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                                .hoverable(jumpCancelHover)
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
                val directionHover = remember { MutableInteractionSource() }
                val directionHovered by directionHover.collectIsHoveredAsState()
                Box(
                    modifier = Modifier
                        .clip(SquircleShape(6.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                        .background(if (directionHovered) MiuixTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                        .hoverable(directionHover)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable {
                            DesktopSettings.readingDirection.value = readingDirection.toggle().name
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = if (readingDirection == DesktopReadingDirection.RTL) {
                            stringResource(MR.strings.settings_reading_direction_rtl)
                        } else {
                            stringResource(MR.strings.settings_reading_direction_ltr)
                        },
                        color = MiuixTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .pointerHoverIcon(PointerIcon.Hand),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(MR.strings.desktop_a11y_close),
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
                                // 未缩放时点击左/右半区翻页（方向随阅读方向设置）
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
                val retryLabel = stringResource(MR.strings.action_retry)
                val toggleReadingDirLabel = if (readingDirection == DesktopReadingDirection.RTL) {
                    stringResource(MR.strings.settings_reading_direction_ltr)
                } else {
                    stringResource(MR.strings.settings_reading_direction_rtl)
                }
                val coroutineScope = rememberCoroutineScope()
                when {
                    url != null -> ContextMenuArea(
                        items = {
                            buildList {
                                if (canPrev) {
                                    add(ContextMenuItem(prevPageLabel) { page += prevDelta })
                                }
                                if (canNext) {
                                    add(ContextMenuItem(nextPageLabel) { page += nextDelta })
                                }
                                add(ContextMenuItem(retryLabel) { reloadKey += 1 })
                                add(
                                    ContextMenuItem(toggleReadingDirLabel) {
                                        DesktopSettings.readingDirection.value = readingDirection.toggle().name
                                    },
                                )
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
                    // 无链接 / 链接解析失败此前仅赋值未渲染（用户只见空白视口），补齐可见占位卡；失败时提供重载
                    linksState != null && imageState == null -> Box(
                        modifier = Modifier
                            .clip(SquircleShape(12.dp))
                            .background(MiuixTheme.colorScheme.surface.copy(alpha = 0.85f))
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = linksState!!,
                                color = if (linksStateIsError) {
                                    MiuixTheme.colorScheme.error
                                } else {
                                    MiuixTheme.colorScheme.onSurface
                                },
                                fontSize = 13.sp,
                            )
                            if (linksStateIsError) {
                                val linksRetryHover = remember { MutableInteractionSource() }
                                val linksRetryHovered by linksRetryHover.collectIsHoveredAsState()
                                Box(
                                    modifier = Modifier
                                        .clip(SquircleShape(8.dp))
                                        .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                                        .background(if (linksRetryHovered) MiuixTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                                        .hoverable(linksRetryHover)
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .clickable { linksReloadKey += 1 }
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = retryLabel,
                                        color = MiuixTheme.colorScheme.primary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }
                    imageState != null -> Box(
                        modifier = Modifier
                            .clip(SquircleShape(12.dp))
                            .background(MiuixTheme.colorScheme.surface.copy(alpha = 0.85f))
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = imageState!!,
                                color = if (imageStateIsError) {
                                    MiuixTheme.colorScheme.error
                                } else {
                                    MiuixTheme.colorScheme.onSurface
                                },
                                fontSize = 13.sp,
                            )
                            val retryHover = remember { MutableInteractionSource() }
                            val retryHovered by retryHover.collectIsHoveredAsState()
                            Box(
                                modifier = Modifier
                                    .clip(SquircleShape(8.dp))
                                    .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                                    .background(if (retryHovered) MiuixTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                                    .hoverable(retryHover)
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
                    }
                    else -> Box(
                        modifier = Modifier
                            .clip(SquircleShape(8.dp))
                            .background(MiuixTheme.colorScheme.surface.copy(alpha = 0.75f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = loadingText,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            fontSize = 13.sp,
                        )
                    }
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
                ReaderNavPill(
                    text = prevPageLabel,
                    enabled = canPrev,
                    onClick = { page += prevDelta },
                )
                if (pageLinks.isNotEmpty()) {
                    val jumpInteraction = remember { MutableInteractionSource() }
                    val jumpHovered by jumpInteraction.collectIsHoveredAsState()
                    Box(
                        modifier = Modifier
                            .clip(SquircleShape(6.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                            .background(if (jumpHovered) MiuixTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable(
                                interactionSource = jumpInteraction,
                                indication = null,
                            ) {
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
                ReaderNavPill(
                    text = nextPageLabel,
                    enabled = canNext,
                    onClick = { page += nextDelta },
                )
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
                    val noticeInteraction = remember { MutableInteractionSource() }
                    val noticeHovered by noticeInteraction.collectIsHoveredAsState()
                    Box(
                        modifier = Modifier
                            .clip(SquircleShape(8.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                            .background(if (noticeHovered) MiuixTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable(
                                interactionSource = noticeInteraction,
                                indication = null,
                            ) {
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
                                val actionHover = remember { MutableInteractionSource() }
                                val actionHovered by actionHover.collectIsHoveredAsState()
                                Text(
                                    text = notice.actionLabel,
                                    color = MiuixTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = if (actionHovered) TextDecoration.Underline else null,
                                    modifier = Modifier
                                        .hoverable(actionHover)
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
    val badgeHover = remember { MutableInteractionSource() }
    val badgeHovered by badgeHover.collectIsHoveredAsState()
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.85f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f), SquircleShape(6.dp))
            .background(Color.White.copy(alpha = if (badgeHovered) 0.12f else 0f), SquircleShape(6.dp))
            .hoverable(badgeHover)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable { onReset() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

// 底部导航翻页胶囊：禁用态半透明衬底 + Default 光标，悬停 primary 半透明叠加与全局操作按钮 token 一致
@Composable
private fun ReaderNavPill(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    DesktopHoverPill(
        onClick = onClick,
        enabled = enabled,
        shape = SquircleShape(8.dp),
        containerColor = if (enabled) {
            MiuixTheme.colorScheme.surfaceContainerHighest
        } else {
            MiuixTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f)
        },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    ) { _ ->
        Text(
            text = text,
            color = if (enabled) {
                MiuixTheme.colorScheme.primary
            } else {
                MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.4f)
            },
            fontSize = 13.sp,
        )
    }
}
