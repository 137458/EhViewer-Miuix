package com.ehviewer.core.ui.effect

import androidx.compose.ui.graphics.Brush

actual fun isRuntimeShaderSupported(): Boolean = false

actual class RuntimeShaderCompat actual constructor(sksl: String) {
    actual fun setFloatUniform(name: String, value: Float): Unit = unsupported()

    actual fun setFloatUniform(name: String, values: FloatArray): Unit = unsupported()

    actual val brush: Brush
        get() = unsupported()

    private fun unsupported(): Nothing = throw UnsupportedOperationException("RuntimeShader is not supported on desktop")
}
