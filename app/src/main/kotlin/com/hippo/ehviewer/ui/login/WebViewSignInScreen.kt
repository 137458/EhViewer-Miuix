package com.hippo.ehviewer.ui.login

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ehviewer.core.i18n.R
import com.ehviewer.core.network.EhCookieStore
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.util.WebInjectionHelper
import com.hippo.ehviewer.util.setDefaultSettings
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 与 LiquidGlassSurface 一致的玻璃提亮描边（上亮下暗）
private val GLASS_RIM = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.8f), Color.White.copy(alpha = 0.1f)),
)
private val GLASS_BLUR_RADIUS = 18.dp

/**
 * 登录页磨砂按钮着色器：只对返回按钮所在的圆区做渐进模糊，其余像素原样透出。
 * WebView 不参与 Backdrop 采样（逐帧重录会与其硬件渲染层冲突导致闪烁），
 * 模糊作为 GPU 后处理直接作用于 WebView 自身图层，每帧仅绘制一次。
 */
private const val LOGIN_BUTTON_BLUR_SKSL = """
    uniform shader content;
    uniform float2 uSize;
    uniform float uRadius;
    uniform float3 uButton;

    const int TAPS = 32;
    const float GOLDEN_ANGLE = 2.39996323;

    half4 main(float2 coord) {
        float mask = 0.0;
        if (uButton.z > 0.0) {
            float d = distance(coord, uButton.xy);
            mask = 1.0 - smoothstep(uButton.z * 0.75, uButton.z, d);
        }
        float radius = uRadius * mask;
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

private class LoginGlassEffectHolder {
    private var shader: RuntimeShader? = null
    private var effect: RenderEffect? = null
    private var lastSize = IntSize.Zero
    private var lastRadiusPx = Float.NaN
    private var lastButton = Offset(Float.NaN, Float.NaN)

    fun get(size: IntSize, radiusPx: Float, buttonCenter: Offset, buttonRadiusPx: Float): RenderEffect? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val current = shader ?: runCatching { RuntimeShader(LOGIN_BUTTON_BLUR_SKSL) }.getOrNull() ?: return null
        shader = current
        if (size != lastSize || radiusPx != lastRadiusPx || buttonCenter != lastButton) {
            current.setFloatUniform("uSize", floatArrayOf(size.width.toFloat(), size.height.toFloat()))
            current.setFloatUniform("uRadius", radiusPx)
            current.setFloatUniform("uButton", floatArrayOf(buttonCenter.x, buttonCenter.y, buttonRadiusPx))
            effect = AndroidRenderEffect
                .createRuntimeShaderEffect(current, "content")
                .asComposeRenderEffect()
            lastSize = size
            lastRadiusPx = radiusPx
            lastButton = buttonCenter
        }
        return effect
    }
}

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.WebViewSignInScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val state = rememberWebViewState(url = EhUrl.URL_SIGN_IN)
    var isHandlingLogin by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        snapshotFlow { !state.isLoading }.collect { hasFinished ->
            if (hasFinished && !isHandlingLogin) {
                state.webView?.evaluateJavascript(WebInjectionHelper.VIEWPORT_META_INJECTION_SCRIPT, null)
                if (EhCookieStore.isCloudflareBypassed()) {
                    Settings.desktopSite.value = false
                }
                if (EhCookieStore.hasSignedIn()) {
                    isHandlingLogin = true
                    EhCookieStore.flush()
                    postLogin().await()
                    navigator.popBackStack()
                }
            }
        }
    }

    // 登录页极简：不做顶栏，仅全屏网页内容 + 液态玻璃返回按钮
    val surfaceContainer = MiuixTheme.colorScheme.surfaceContainer
    val density = LocalDensity.current
    val blurRadiusPx = with(density) { GLASS_BLUR_RADIUS.toPx() }
    val buttonRadiusPx = with(density) { 20.dp.toPx() }
    var layerSize by remember { mutableStateOf(IntSize.Zero) }
    var buttonCenter by remember { mutableStateOf(Offset.Zero) }
    var webViewOffset by remember { mutableStateOf(Offset.Zero) }
    val effectHolder = remember { LoginGlassEffectHolder() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background),
    ) {
        WebView(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .onGloballyPositioned { webViewOffset = it.positionInRoot() }
                .onSizeChanged { layerSize = it }
                .graphicsLayer {
                    renderEffect = effectHolder.get(
                        layerSize,
                        blurRadiusPx,
                        buttonCenter - webViewOffset,
                        buttonRadiusPx,
                    )
                },
            onCreated = {
                EhUtils.signOut()
                it.setDefaultSettings()
                it.evaluateJavascript(WebInjectionHelper.VIEWPORT_META_INJECTION_SCRIPT, null)
            },
        )
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 12.dp, top = 8.dp)
                .size(40.dp)
                .onGloballyPositioned {
                    buttonCenter = it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f)
                }
                .background(surfaceContainer.copy(alpha = 0.5f), CircleShape)
                .border(1.dp, GLASS_RIM, CircleShape)
                .clickable { navigator.popBackStack() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = MiuixIcons.Back, contentDescription = null, tint = MiuixTheme.colorScheme.onSurface)
        }
    }
}
