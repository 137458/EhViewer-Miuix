package com.hippo.ehviewer.ui.legacy

import android.graphics.drawable.DrawableWrapper
import android.text.Html
import coil3.asDrawable
import coil3.decode.DecodeUtils
import coil3.imageLoader
import coil3.request.crossfade
import coil3.size.Scale
import coil3.size.Size
import com.ehviewer.core.util.toIntOrDefault
import com.hippo.ehviewer.ktbuilder.imageRequest
import kotlin.math.roundToInt
import splitties.init.appCtx

private val UrlRegex = "(?:[^-]+-){2}(\\d+)-(\\d+)-[^_]+_([^.]+)".toRegex()

class CoilImageGetter(private val onSuccess: () -> Unit) : Html.ImageGetter {
    override fun getDrawable(source: String) = object : DrawableWrapper(null) {}.apply {
        UrlRegex.find(source)?.run {
            // 注释里的图片 URL 可能被构造得残缺，解析失败或尺寸非法时退回默认占位尺寸，
            // 否则会抛 NumberFormatException/除零把整段评论渲染打断
            val srcWidth = groupValues[1].toIntOrNull()
            val srcHeight = groupValues[2].toIntOrNull()
            if (srcWidth != null && srcHeight != null && srcWidth > 0 && srcHeight > 0) {
                val dstWidth = groupValues[3].toIntOrDefault(200).coerceAtLeast(1)
                val dstHeight = dstWidth / 2 * 3
                val multiplier = DecodeUtils.computeSizeMultiplier(srcWidth, srcHeight, dstWidth, dstHeight, Scale.FIT, Size.ORIGINAL)
                setBounds(0, 0, (srcWidth * multiplier).roundToInt(), (srcHeight * multiplier).roundToInt())
            } else {
                setBounds(0, 0, 200, 300)
            }
        } ?: setBounds(0, 0, 200, 300)
        with(appCtx) {
            imageLoader.enqueue(
                imageRequest {
                    data(source)
                    crossfade(false)
                    target { image ->
                        setDrawable(image.asDrawable(resources))
                        onSuccess()
                    }
                },
            )
        }
    }
}
