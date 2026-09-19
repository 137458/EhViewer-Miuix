package com.ehviewer.core.ui.component

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.anchoredHorizontalDraggable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.ehviewer.core.ui.util.animateFloatMergeOneWayPredictiveBackAsState
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val SideSheetAnimationSpec = TweenSpec<Float>(durationMillis = 256)

enum class SideSheetValue {
    Closed,
    Open,
}

@Stable
class SideSheetState(
    initialValue: SideSheetValue = SideSheetValue.Closed,
) {
    internal val anchoredDraggableState = AnchoredDraggableState(
        initialValue = initialValue,
    )

    val isOpen: Boolean
        get() = currentValue == SideSheetValue.Open

    val isClosed: Boolean
        get() = currentValue == SideSheetValue.Closed

    val currentValue: SideSheetValue
        get() = anchoredDraggableState.settledValue

    val isAnimationRunning: Boolean
        get() = anchoredDraggableState.isAnimationRunning

    val targetValue: SideSheetValue
        get() = anchoredDraggableState.targetValue

    val currentOffset: Float
        get() = anchoredDraggableState.offset

    internal var density: Density? by mutableStateOf(null)

    internal fun requireOffset(): Float = anchoredDraggableState.requireOffset()

    suspend fun open() = animateTo(SideSheetValue.Open)

    suspend fun close() = animateTo(SideSheetValue.Closed)

    suspend fun animateTo(
        targetValue: SideSheetValue,
        animationSpec: AnimationSpec<Float> = SideSheetAnimationSpec,
        velocity: Float = anchoredDraggableState.lastVelocity,
    ) {
        anchoredDraggableState.anchoredDrag(targetValue = targetValue) { anchors, latestTarget ->
            val targetOffset = anchors.positionOf(latestTarget)
            if (!targetOffset.isNaN()) {
                var prev = if (currentOffset.isNaN()) 0f else currentOffset
                animate(prev, targetOffset, velocity, animationSpec) { value, vel ->
                    dragTo(value, vel)
                    prev = value
                }
            }
        }
    }

    suspend fun snapTo(targetValue: SideSheetValue) {
        anchoredDraggableState.snapTo(targetValue)
    }

    companion object {
        val Saver = Saver<SideSheetState, SideSheetValue>(
            save = { it.currentValue },
            restore = { SideSheetState(it) },
        )
    }
}

typealias DrawerState2 = SideSheetState

@Composable
fun rememberSideSheetState(
    initialValue: SideSheetValue = SideSheetValue.Closed,
): SideSheetState = rememberSaveable(saver = SideSheetState.Saver) {
    SideSheetState(initialValue)
}

@Composable
fun rememberDrawerState2(
    initialValue: SideSheetValue = SideSheetValue.Closed,
): SideSheetState = rememberSideSheetState(initialValue)

private typealias Sheet = @Composable ColumnScope.(SideSheetState) -> Unit

val LocalSideSheetContainer = staticCompositionLocalOf<SnapshotStateList<Sheet>> { error("SideSheetContainer not present") }
val LocalSideSheetState = staticCompositionLocalOf<SideSheetState> { error("SideSheetState not present") }

@Composable
fun ProvideSideSheetContent(content: Sheet) {
    val container = LocalSideSheetContainer.current
    DisposableEffect(content) {
        container.add(0, content)
        onDispose { container.remove(content) }
    }
}

@Composable
fun MutableSideSheet(
    drawerState: SideSheetState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val sheet = remember { mutableStateListOf<Sheet>() }
    val f = sheet.firstOrNull()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val windowInfo = LocalWindowInfo.current
    val width = with(density) { windowInfo.containerSize.width.toDp() }
    val height = with(density) { windowInfo.containerSize.height.toDp() }
    val maxSheetWidth = (width - 112.dp).coerceAtLeast(280.dp)
    // 横屏矮视口下 width-112dp 会占掉近乎整屏，侧栏按固定上限收窄
    val effectiveSheetWidth = if (height < 600.dp && width > height) {
        maxSheetWidth.coerceAtMost(420.dp)
    } else {
        maxSheetWidth
    }
    var maxValue by remember { mutableFloatStateOf(with(density) { 360.dp.toPx() }) }
    val minValue = 0f
    val gesturesEnabled = f != null && enabled

    SideEffect {
        drawerState.density = density
        drawerState.anchoredDraggableState.updateAnchors(
            DraggableAnchors {
                SideSheetValue.Closed at maxValue
                SideSheetValue.Open at minValue
            },
        )
    }

    CompositionLocalProvider(
        LocalSideSheetContainer provides sheet,
        LocalSideSheetState provides drawerState,
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .anchoredHorizontalDraggable(
                    state = drawerState.anchoredDraggableState,
                    enableDragFromStartToEnd = drawerState.isOpen,
                    enableDragFromEndToStart = drawerState.isClosed,
                    enabled = gesturesEnabled,
                    flingBehavior = AnchoredDraggableDefaults.flingBehavior(
                        state = drawerState.anchoredDraggableState,
                        animationSpec = SideSheetAnimationSpec,
                    ),
                ),
        ) {
            val radius by remember {
                snapshotFlow {
                    val step = calculateFraction(maxValue, minValue, drawerState.currentOffset)
                    lerp(0, 10, step).dp
                }
            }.collectAsState(0.dp)
            val blurModifier = Modifier.graphicsLayer {
                if (radius != 0.dp) {
                    renderEffect = BlurEffect(radius.toPx(), radius.toPx(), TileMode.Clamp)
                    shape = RectangleShape
                    clip = true
                }
            }
            Box(modifier = Modifier.fillMaxSize().then(blurModifier)) {
                content()
            }

            if (f != null) {
                Scrim(
                    open = drawerState.isOpen,
                    onClose = {
                        if (gesturesEnabled) {
                            scope.launch { drawerState.close() }
                        }
                    },
                    fraction = {
                        calculateFraction(maxValue, minValue, drawerState.requireOffset())
                    },
                    color = MiuixTheme.colorScheme.windowDimming,
                )

                val predictiveState by animateFloatMergeOneWayPredictiveBackAsState(drawerState.isOpen) {
                    drawerState.close()
                }

                val predictiveModifier = if (drawerState.isOpen) {
                    if (predictiveState < 0) {
                        Modifier.layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            val multiplierX = lerp(1f, 1.05f, -predictiveState)
                            val multiplierY = lerp(1f, 0.95f, -predictiveState)
                            val scaledWidth = (multiplierX * placeable.width).roundToInt()
                            val scaledHeight = (multiplierY * placeable.height).roundToInt()
                            val reMeasured = measurable.measure(Constraints.fixed(scaledWidth, scaledHeight))
                            layout(scaledWidth, scaledHeight) {
                                reMeasured.placeRelative(placeable.width - scaledWidth, 0)
                            }
                        }
                    } else {
                        Modifier.graphicsLayer {
                            val scale = lerp(1f, 0.95f, predictiveState)
                            scaleX = scale
                            scaleY = scale
                        }.offset {
                            IntOffset(lerp(0f, maxValue * 0.05f, predictiveState).roundToInt(), 0)
                        }
                    }
                } else {
                    Modifier.onSizeChanged {
                        val w = it.width
                        if (w != 0) {
                            maxValue = w.toFloat()
                            drawerState.anchoredDraggableState.updateAnchors(
                                DraggableAnchors {
                                    SideSheetValue.Closed at maxValue
                                    SideSheetValue.Open at minValue
                                },
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .offset {
                            val off = if (drawerState.anchoredDraggableState.offset.isNaN()) maxValue else drawerState.requireOffset()
                            IntOffset(off.roundToInt(), 0)
                        }
                        .align(Alignment.CenterEnd)
                        .then(predictiveModifier)
                        .widthIn(max = effectiveSheetWidth)
                        .fillMaxHeight()
                        .background(
                            color = MiuixTheme.colorScheme.surface,
                            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                        )
                        .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.End + WindowInsetsSides.Bottom)),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        f(drawerState)
                    }
                }
            }
        }
    }
}

private fun calculateFraction(a: Float, b: Float, pos: Float) = ((pos - a) / (b - a)).coerceIn(0f, 1f)

@Composable
private fun Scrim(open: Boolean, onClose: () -> Unit, fraction: () -> Float, color: Color) {
    val dismiss = if (open) {
        Modifier.pointerInput(onClose) { detectTapGestures { onClose() } }
    } else {
        Modifier
    }
    Canvas(Modifier.fillMaxSize().then(dismiss)) {
        drawRect(color, alpha = fraction())
    }
}
