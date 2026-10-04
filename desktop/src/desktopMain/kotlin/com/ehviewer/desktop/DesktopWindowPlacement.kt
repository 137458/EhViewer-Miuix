package com.ehviewer.desktop

import java.awt.Rectangle

// 窗口位置保存前的可见性校验：任一屏幕包含该坐标即可恢复，避免外接显示器拔除后窗口恢复到屏幕外不可见
fun isPositionWithinScreens(x: Int, y: Int, screens: List<Rectangle>): Boolean = screens.any { it.contains(x, y) }
