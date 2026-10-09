package com.hippo.ehviewer.ui.main

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.webkit.WebView as AndroidWebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ehviewer.core.ui.effect.isRuntimeShaderSupported
import com.google.accompanist.web.WebView
import com.google.accompanist.web.WebViewNavigator
import com.google.accompanist.web.WebViewState
import com.google.accompanist.web.rememberWebViewNavigator
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val WebViewTopBlurRadius = 20.dp

// 与 BlurredBar 的 surface 提亮一致，保证状态栏/标题在模糊内容上的对比度
private const val WEBVIEW_TOP_TINT_ALPHA = 0.3f

/**
 * AGSL 渐进模糊：uFadeEnd 以上满额模糊，向下按 smoothstep 渐隐到 0，与 BlurredBar 的
 * ProgressiveBlur.Top 观感对齐。采样坐标钳制在图层内，避免越界采到透明色产生暗边。
 */
private const val WEBVIEW_PROGRESSIVE_BLUR_SKSL = """
    uniform shader content;
    uniform float2 uSize;
    uniform float uFadeEnd;
    uniform float uRadius;

    const int TAPS = 32;
    const float GOLDEN_ANGLE = 2.39996323;

    half4 main(float2 coord) {
        float fadeEnd = max(uFadeEnd, 1.0);
        float t = clamp(1.0 - coord.y / fadeEnd, 0.0, 1.0);
        float fade = t * t * (3.0 - 2.0 * t);
        float radius = uRadius * fade;
        if (radius < 0.5) {
            return content.eval(coord);
        }
        half4 sum = half4(0.0);
        for (int i = 0; i < TAPS; i++) {
            float fi = float(i);
            float angle = fi * GOLDEN_ANGLE;
            float dist = radius * sqrt((fi + 0.5) / float(TAPS));
            float2 offset = float2(cos(angle), sin(angle)) * dist;
            float2 p = clamp(coord + offset, float2(0.0), uSize - float2(1.0));
            sum += content.eval(p);
        }
        return sum * (1.0 / float(TAPS));
    }
"""

/**
 * 内嵌 WebView 页面的统一脚手架。
 *
 * WebView 不接入 BlurredBar 的 layerBackdrop 采样：逐帧把 WebView 重录进 GraphicsLayer
 * 会与其硬件加速渲染层冲突导致页面持续闪烁。这里改为 WebView 平铺到顶栏下方，AGSL 渐变
 * 模糊作为 GPU 后处理直接作用于其自身图层（每帧仅绘制一次，不走采样重录）。
 */
private class WebViewBlurEffectHolder {
    private var shader: RuntimeShader? = null
    private var effect: RenderEffect? = null
    private var lastSize = IntSize.Zero
    private var lastFadeEndPx = Float.NaN
    private var lastRadiusPx = Float.NaN

    fun get(size: IntSize, fadeEndPx: Float, radiusPx: Float): RenderEffect? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val current = shader ?: runCatching { RuntimeShader(WEBVIEW_PROGRESSIVE_BLUR_SKSL) }.getOrNull() ?: return null
        shader = current
        if (size != lastSize || fadeEndPx != lastFadeEndPx || radiusPx != lastRadiusPx) {
            current.setFloatUniform("uSize", floatArrayOf(size.width.toFloat(), size.height.toFloat()))
            current.setFloatUniform("uFadeEnd", fadeEndPx)
            current.setFloatUniform("uRadius", radiusPx)
            effect = AndroidRenderEffect
                .createRuntimeShaderEffect(current, "content")
                .asComposeRenderEffect()
            lastSize = size
            lastFadeEndPx = fadeEndPx
            lastRadiusPx = radiusPx
        }
        return effect
    }
}

/**
 * 顶栏悬浮于 WebView 之上，页面顶部经渐变模糊后从顶栏下透出；API < 33（无 AGSL）退回
 * 实色顶栏 + 内容避开顶栏的布局。
 *
 * @param title 顶栏标题
 * @param state WebView 状态
 * @param onCreated WebView 创建回调（设置、注入等）
 * @param navigationIcon 顶栏导航图标（NavigationIcon 依赖 DestinationsNavigator 上下文，由调用方提供）
 * @param navigator WebView 导航器（需外部驱动导航时传入）
 * @param actions 顶栏尾部操作
 */
@Composable
fun WebViewScaffold(
    title: String,
    state: WebViewState,
    onCreated: (AndroidWebView) -> Unit,
    navigationIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigator: WebViewNavigator = rememberWebViewNavigator(),
    actions: @Composable RowScope.() -> Unit = {},
) {
    if (!remember { isRuntimeShaderSupported() }) {
        WebViewScaffoldFallback(title, state, onCreated, navigationIcon, modifier, navigator, actions)
        return
    }
    val surface = MiuixTheme.colorScheme.surface
    val radiusPx = with(LocalDensity.current) { WebViewTopBlurRadius.toPx() }
    var layerSize by remember { mutableStateOf(IntSize.Zero) }
    val effectHolder = remember { WebViewBlurEffectHolder() }

    Scaffold(modifier = modifier, topBar = {
        Box {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(surface.copy(alpha = WEBVIEW_TOP_TINT_ALPHA)),
            )
            TopAppBar(
                title = title,
                navigationIcon = navigationIcon,
                color = Color.Transparent,
                actions = actions,
            )
        }
    }) { paddingValues ->
        val fadeEndPx = with(LocalDensity.current) { paddingValues.calculateTopPadding().toPx() }
        WebView(
            state = state,
            navigator = navigator,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
                .onSizeChanged { layerSize = it }
                .graphicsLayer {
                    renderEffect = effectHolder.get(layerSize, fadeEndPx, radiusPx)
                },
            onCreated = onCreated,
        )
    }
}

@Composable
private fun WebViewScaffoldFallback(
    title: String,
    state: WebViewState,
    onCreated: (AndroidWebView) -> Unit,
    navigationIcon: @Composable () -> Unit,
    modifier: Modifier,
    navigator: WebViewNavigator,
    actions: @Composable RowScope.() -> Unit,
) {
    Scaffold(modifier = modifier, topBar = {
        TopAppBar(
            title = title,
            navigationIcon = navigationIcon,
            color = MiuixTheme.colorScheme.surface,
            actions = actions,
        )
    }) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background),
        ) {
            WebView(
                state = state,
                navigator = navigator,
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                onCreated = onCreated,
            )
        }
    }
}
