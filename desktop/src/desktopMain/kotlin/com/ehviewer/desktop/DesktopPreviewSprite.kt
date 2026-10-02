package com.ehviewer.desktop

// V2 预览雪碧图单元格几何：sprite 为单行等宽格，按目标显示高推算缩放与水平位移
object DesktopPreviewSprite {
    data class CellLayout(val offsetX: Float, val width: Float, val height: Float)

    fun cellLayout(offsetX: Int, clipWidth: Int, clipHeight: Int, displayHeight: Float): CellLayout? {
        if (clipWidth <= 0 || clipHeight <= 0) {
            // 退化元数据：回退方形布局，避免除零
            return CellLayout(0f, displayHeight, displayHeight)
        }
        val scale = displayHeight / clipHeight
        return CellLayout(
            offsetX = -offsetX * scale,
            width = clipWidth * scale,
            height = displayHeight,
        )
    }
}
