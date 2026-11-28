package com.hippo.ehviewer.ui.reader

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.util.fastRoundToInt

class BitmapPainter(private val bitmap: Bitmap, override val intrinsicSize: Size) : Painter() {
    private val srcRect = intrinsicSize.toRect().toAndroidRectF()
    private val dstRect = RectF()
    private val matrix = Matrix()

    // 每个 Painter 实例持有自己的 Paint：共享同一个全局 Paint 会让 alpha/colorFilter
    // 在并发绘制的不同 Composable 之间互相污染
    private val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        isDither = true
    }

    // Use the overload that takes a `Matrix` to bypass the 100 MB size limit
    override fun DrawScope.onDraw() = drawIntoCanvas { canvas ->
        dstRect.right = size.width.fastRoundToInt().toFloat()
        dstRect.bottom = size.height.fastRoundToInt().toFloat()
        matrix.setRectToRect(srcRect, dstRect, Matrix.ScaleToFit.FILL)
        canvas.nativeCanvas.drawBitmap(bitmap, matrix, paint)
    }

    override fun applyAlpha(alpha: Float): Boolean {
        paint.alpha = (alpha.coerceIn(0f, 1f) * 255f).fastRoundToInt()
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        paint.colorFilter = colorFilter?.asAndroidColorFilter()
        return true
    }
}
