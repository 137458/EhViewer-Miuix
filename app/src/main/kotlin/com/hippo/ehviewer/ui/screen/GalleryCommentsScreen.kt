package com.hippo.ehviewer.ui.screen

import android.graphics.Typeface
import android.text.Html
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import androidx.annotation.ColorInt
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import com.ehviewer.core.ui.component.blurBackdropSource
import com.ehviewer.core.ui.component.rememberBlurBackdrop
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.util.lerp
import androidx.core.text.buildSpannedString
import androidx.core.text.getSpans
import androidx.core.text.inSpans
import androidx.core.text.parseAsHtml
import com.ehviewer.core.database.model.Filter
import com.ehviewer.core.database.model.FilterMode
import com.ehviewer.core.i18n.R
import com.ehviewer.core.model.GalleryComment
import com.ehviewer.core.model.GalleryDetail
import com.ehviewer.core.ui.component.BlurredBar
import com.ehviewer.core.ui.util.animateFloatMergePredictiveBackAsState
import com.ehviewer.core.ui.util.snackBarPadding
import com.ehviewer.core.ui.util.thenIf
import com.ehviewer.core.util.isAtLeastP
import com.ehviewer.core.util.launch
import com.ehviewer.core.util.launchIO
import com.ehviewer.core.util.logcat
import com.ehviewer.core.util.withUIContext
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhFilter.remember
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.jumpToReaderByPage
import com.hippo.ehviewer.ui.main.GalleryCommentCard
import com.hippo.ehviewer.ui.main.NavigationIcon
import com.hippo.ehviewer.ui.openBrowser
import com.hippo.ehviewer.ui.tools.addBBCodeTextContextMenuItems
import com.hippo.ehviewer.ui.tools.awaitConfirmationOrCancel
import com.hippo.ehviewer.ui.tools.awaitSelectAction
import com.hippo.ehviewer.ui.tools.normalizeSpan
import com.hippo.ehviewer.ui.tools.showNoButton
import com.hippo.ehviewer.ui.tools.toBBCode
import com.hippo.ehviewer.ui.tools.updateSpan
import com.hippo.ehviewer.util.ReadableTime
import com.hippo.ehviewer.util.addTextToClipboard
import com.hippo.ehviewer.util.displayString
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlin.math.roundToInt
import moe.tarsin.coroutines.runSuspendCatching
import moe.tarsin.navigate
import moe.tarsin.snackbar
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Reply
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val URL_PATTERN = Regex("(http|https)://[a-z0-9A-Z%-]+(\\.[a-z0-9A-Z%-]+)+(:\\d{1,5})?(/[a-zA-Z0-9-_~:#@!&',;=%/*.?+$\\[\\]()]+)?/?")

private inline fun SpannableStringBuilder.withSpans(
    @ColorInt color: Int,
    builderAction: SpannableStringBuilder.() -> Unit,
) = inSpans(
    RelativeSizeSpan(0.8f),
    StyleSpan(Typeface.BOLD),
    ForegroundColorSpan(color),
    builderAction = builderAction,
)

@Composable
fun processComment(
    comment: GalleryComment,
    imageGetter: Html.ImageGetter,
) = comment.comment.parseAsHtml(imageGetter = imageGetter).let { text ->
    buildSpannedString {
        append(text)
        URL_PATTERN.findAll(text).forEach { result ->
            val start = result.range.first
            val end = result.range.last + 1
            if (getSpans<URLSpan>(start, end).isEmpty()) {
                setSpan(URLSpan(result.groupValues[0]), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        val color = MiuixTheme.colorScheme.onSurfaceVariantSummary.toArgb()
        if (comment.id != 0L && comment.score != 0) {
            val score = comment.score
            val scoreString = if (score > 0) "+$score" else score.toString()
            append("  ")
            withSpans(color) {
                append(scoreString)
            }
        }
        if (comment.lastEdited != 0L) {
            append("\n\n")
            withSpans(color) {
                append(
                    stringResource(
                        R.string.last_edited,
                        ReadableTime.getTimeAgo(comment.lastEdited),
                    ),
                )
            }
        }
    }
}

sealed interface GalleryCommentsState {
    data class Ready(val galleryDetail: GalleryDetail) : GalleryCommentsState
    data class NeedsFetch(val gid: Long, val token: String) : GalleryCommentsState
    data object MissingDetail : GalleryCommentsState
}

fun resolveGalleryCommentsState(
    gid: Long,
    token: String?,
    cachedDetail: GalleryDetail?,
): GalleryCommentsState {
    if (cachedDetail != null) {
        return GalleryCommentsState.Ready(cachedDetail)
    }
    if (!token.isNullOrBlank()) {
        return GalleryCommentsState.NeedsFetch(gid, token)
    }
    return GalleryCommentsState.MissingDetail
}

private val MinimumContentPaddingEditText = 88.dp

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.GalleryCommentsScreen(
    gid: Long,
    detailToken: String? = null,
    navigator: DestinationsNavigator,
) = Screen(navigator) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val commentingState = rememberSaveable { mutableStateOf(false) }
    var commenting by commentingState
    val animationProgress by animateFloatMergePredictiveBackAsState(enable = commenting) { commenting = false }
    val animateItems by Settings.animateItems.collectAsState()

    var recoveredDetail by remember { mutableStateOf<GalleryDetail?>(null) }
    val rawCachedDetail = detailCache[gid] ?: recoveredDetail
    val commentsState = remember(gid, detailToken, rawCachedDetail) {
        resolveGalleryCommentsState(gid, detailToken, rawCachedDetail)
    }

    LaunchedEffect(commentsState) {
        if (commentsState is GalleryCommentsState.NeedsFetch) {
            runCatching {
                val url = EhUrl.getGalleryDetailUrl(commentsState.gid, commentsState.token)
                val fetched = EhEngine.getGalleryDetail(url)
                detailCache.put(fetched.gid, fetched)
                recoveredDetail = fetched
            }
        }
    }

    val galleryDetail = (commentsState as? GalleryCommentsState.Ready)?.galleryDetail ?: recoveredDetail

    if (galleryDetail == null) {
        Scaffold(
            topBar = {
                BlurredBar(
                    backdrop = backdrop,
                ) {
                    TopAppBar(
                        title = stringResource(id = R.string.gallery_comments),
                        navigationIcon = { NavigationIcon() },
                        color = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface,
                    )
                }
            },
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blurBackdropSource(backdrop)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                if (commentsState is GalleryCommentsState.NeedsFetch) {
                    InfiniteProgressIndicator()
                } else {
                    Text(
                        text = stringResource(id = R.string.error_something_wrong_happened),
                        style = MiuixTheme.textStyles.body1,
                    )
                }
            }
        }
    } else {
        val userCommentBackField = remember { mutableStateOf(TextFieldValue()) }
        var userComment by userCommentBackField
        var commentId by remember { mutableLongStateOf(-1) }
        var comments by remember(galleryDetail) { mutableStateOf(galleryDetail.comments) }
        LaunchedEffect(comments) {
            galleryDetail.comments = comments
        }
        var refreshing by remember { mutableStateOf(false) }
        val density = LocalDensity.current

        suspend fun refreshComment(showAll: Boolean) {
            val url = EhUrl.getGalleryDetailUrl(galleryDetail.gid, galleryDetail.token, allComment = showAll)
            val detail = EhEngine.getGalleryDetail(url)
            comments = detail.comments
        }

        val copyComment = stringResource(R.string.copy_comment_text)
        val blockCommenter = stringResource(R.string.block_commenter)
        val editComment = stringResource(R.string.edit_comment)
        val cancelVoteUp = stringResource(R.string.cancel_vote_up)
        val cancelVoteDown = stringResource(R.string.cancel_vote_down)
        val voteUp = stringResource(R.string.vote_up)
        val voteDown = stringResource(R.string.vote_down)
        val checkVoteStatus = stringResource(R.string.check_vote_status)
        val editCommentSuccess = stringResource(R.string.edit_comment_successfully)
        val commentSuccess = stringResource(R.string.comment_successfully)
        val editCommentFail = stringResource(R.string.edit_comment_failed)
        val commentFail = stringResource(R.string.comment_failed)

        val focusManager = LocalFocusManager.current

        suspend fun sendComment() {
            commenting = false
            withUIContext { focusManager.clearFocus() }
            val url = EhUrl.getGalleryDetailUrl(galleryDetail.gid, galleryDetail.token)
            userComment.runSuspendCatching {
                val bbcode = annotatedString.normalizeSpan().toBBCode()
                logcat("sendComment") { bbcode }
                EhEngine.commentGallery(url, bbcode, commentId)
            }.onSuccess {
                val msg = if (commentId != -1L) editCommentSuccess else commentSuccess
                userComment = TextFieldValue()
                commentId = -1L
                comments = it
                snackbar(msg)
            }.onFailure {
                val text = if (commentId != -1L) editCommentFail else commentFail
                snackbar(text + "\n" + it.displayString())
            }
        }

        val filterAdded = stringResource(R.string.filter_added)
        suspend fun showFilterCommenter(comment: GalleryComment) {
            val commenter = comment.user ?: return
            awaitConfirmationOrCancel { Text(text = stringResource(R.string.filter_the_commenter, commenter)) }
            Filter(FilterMode.COMMENTER, commenter).remember()
            comments = comments.copy(comments = comments.comments.filterNot { it.user == commenter })
            snackbar(filterAdded)
        }

        suspend fun showCommentVoteStatus(status: String) {
            val data = status.split(',').map {
                val str = it.trim()
                val index = str.lastIndexOf(' ')
                if (index < 0) {
                    str to ""
                } else {
                    str.substring(0, index).trim() to str.substring(index + 1).trim()
                }
            }
            // Wait cancellation
            showNoButton<Unit> {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp),
                ) {
                    data.forEach { (name, vote) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = name,
                                style = MiuixTheme.textStyles.body1,
                                color = MiuixTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (vote.isNotEmpty()) {
                                Text(
                                    text = vote,
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        }
                    }
                }
            }
        }
        val hasSignedIn by Settings.hasSignedIn.collectAsState()
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                BlurredBar(
                    backdrop = backdrop,
                    scrollBehavior = scrollBehavior,
                ) {
                    TopAppBar(
                        title = stringResource(id = R.string.gallery_comments),
                        navigationIcon = { NavigationIcon() },
                        scrollBehavior = scrollBehavior,
                        color = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface,
                    )
                }
            },
            floatingActionButton = {
                if (hasSignedIn && !commenting) {
                    FloatingActionButton(
                        onClick = {
                            if (commentId != -1L) {
                                commentId = -1L
                                userComment = TextFieldValue()
                            }
                            commenting = true
                        },
                        modifier = Modifier.snackBarPadding(),
                    ) {
                        Icon(imageVector = MiuixIcons.Reply, contentDescription = null)
                    }
                }
            },
        ) { paddingValues ->
            val keylineMargin = dimensionResource(id = com.hippo.ehviewer.R.dimen.keyline_margin)
            var editTextMeasured by remember { mutableStateOf(MinimumContentPaddingEditText) }
            var isRefreshing by remember { mutableStateOf(false) }
            val refreshState = rememberPullToRefreshState()
            PullToRefresh(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    launchIO {
                        runSuspendCatching {
                            refreshComment(true)
                        }
                        isRefreshing = false
                    }
                },
                pullToRefreshState = refreshState,
                modifier = Modifier.imePadding().padding(top = paddingValues.calculateTopPadding()),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blurBackdropSource(backdrop),
                ) {
                    val additionalPadding = if (commenting) {
                        editTextMeasured
                    } else {
                        if (!comments.hasMore) {
                            MinimumContentPaddingEditText
                        } else {
                            0.dp
                        }
                    }
                    val voteUpSucceed = stringResource(R.string.vote_up_successfully)
                    val cancelVoteUpSucceed = stringResource(R.string.cancel_vote_up_successfully)
                    val voteDownSucceed = stringResource(R.string.vote_down_successfully)
                    val cancelVoteDownSucceed = stringResource(R.string.cancel_vote_down_successfully)
                    val voteFailed = stringResource(R.string.vote_failed)
                    val layoutDirection = LocalLayoutDirection.current
                    val lazyListState = rememberLazyListState()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = keylineMargin),
                        state = lazyListState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(
                            start = paddingValues.calculateStartPadding(layoutDirection),
                            end = paddingValues.calculateEndPadding(layoutDirection),
                            bottom = paddingValues.calculateBottomPadding() + additionalPadding,
                        ),
                    ) {
                        items(
                            items = comments.comments,
                            key = { it.id },
                        ) { item ->
                            suspend fun voteComment(comment: GalleryComment, isUp: Boolean) {
                                galleryDetail.runSuspendCatching {
                                    EhEngine.voteComment(apiUid, apiKey, gid, token, comment.id, if (isUp) 1 else -1).also {
                                        refreshComment(true)
                                    }
                                }.onSuccess { result ->
                                    snackbar(
                                        if (isUp) {
                                            if (0 != result.vote) voteUpSucceed else cancelVoteUpSucceed
                                        } else {
                                            if (0 != result.vote) voteDownSucceed else cancelVoteDownSucceed
                                        },
                                    )
                                }.onFailure {
                                    snackbar(voteFailed)
                                }
                            }

                            suspend fun doCommentAction(comment: GalleryComment) = awaitSelectAction {
                                onSelect(copyComment) {
                                    addTextToClipboard(comment.comment.parseAsHtml())
                                }
                                if (!comment.uploader && !comment.editable) {
                                    onSelect(blockCommenter) { showFilterCommenter(comment) }
                                }
                                if (comment.editable) {
                                    onSelect(editComment) {
                                        userComment = TextFieldValue(AnnotatedString.fromHtml(comment.comment))
                                        commentId = comment.id
                                        commenting = true
                                    }
                                }
                                if (comment.voteUpAble) {
                                    onSelect(if (comment.voteUpEd) cancelVoteUp else voteUp) {
                                        voteComment(comment, true)
                                    }
                                }
                                if (comment.voteDownAble) {
                                    onSelect(if (comment.voteDownEd) cancelVoteDown else voteDown) {
                                        voteComment(comment, false)
                                    }
                                }
                                if (!comment.voteState.isNullOrEmpty()) {
                                    onSelect(checkVoteStatus) {
                                        showCommentVoteStatus(comment.voteState!!)
                                    }
                                }
                            }()

                            GalleryCommentCard(
                                modifier = Modifier.thenIf(animateItems) { animateItem() },
                                comment = item,
                                onUserClick = {
                                    navigate(
                                        ListUrlBuilder(
                                            mode = ListUrlBuilder.MODE_UPLOADER,
                                            keyword = item.user,
                                        ).asDst(),
                                    )
                                },
                                onCardClick = { launch { doCommentAction(item) } },
                                onUrlClick = {
                                    if (it.startsWith("#c")) {
                                        it.substring(2).toLongOrNull()?.let { id ->
                                            val index = comments.comments.indexOfFirst { c -> c.id == id }
                                            if (index != -1) {
                                                launch { lazyListState.animateScrollToItem(index) }
                                            }
                                        }
                                    } else {
                                        if (!jumpToReaderByPage(it, galleryDetail)) if (!navWithUrl(it)) openBrowser(it)
                                    }
                                },
                                processComment = { c, ig -> processComment(c, ig) },
                            )
                        }
                        if (comments.hasMore) {
                            item {
                                Crossfade(targetState = refreshing, modifier = Modifier.padding(keylineMargin), label = "refreshing") {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(40.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (it) {
                                            InfiniteProgressIndicator()
                                        } else {
                                            TextButton(
                                                text = stringResource(id = R.string.click_more_comments),
                                                onClick = {
                                                    launchIO {
                                                        refreshing = true
                                                        runSuspendCatching { refreshComment(true) }
                                                        refreshing = false
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (!isAtLeastP) {
                        // Workaround for crash when 0-sized TextField is focused
                        // https://issuetracker.google.com/440964236
                        Box(Modifier.size(1.dp).focusable())
                    }
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).layout { measurable, constraints ->
                            val origin = measurable.measure(constraints)
                            val width = lerp(origin.width, 0, animationProgress)
                            val height = lerp(origin.height, 0, animationProgress)
                            val placeable = measurable.measure(Constraints.fixed(width, height))
                            layout(width, height) {
                                placeable.placeRelative(0, 0)
                            }
                        }.graphicsLayer {
                            shape = RoundedCornerShape((animationProgress * 100).roundToInt())
                            clip = true
                        }.height(IntrinsicSize.Min),
                        color = MiuixTheme.colorScheme.primaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().navigationBarsPadding().onGloballyPositioned { coordinates ->
                                editTextMeasured = max(with(density) { coordinates.size.height.toDp() }, MinimumContentPaddingEditText)
                            },
                        ) {
                            val color = MiuixTheme.colorScheme.onPrimaryContainer
                            BasicTextField(
                                value = userComment,
                                onValueChange = { textFieldValue ->
                                    userComment = textFieldValue.updateSpan(userComment)
                                },
                                modifier = Modifier.weight(1f).padding(keylineMargin).addBBCodeTextContextMenuItems(userCommentBackField),
                                textStyle = MiuixTheme.textStyles.body1.merge(color = color),
                                cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
                            )
                            IconButton(
                                onClick = { launchIO { sendComment() } },
                                modifier = Modifier.align(Alignment.CenterVertically).padding(16.dp),
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Send,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
