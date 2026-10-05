package com.ehviewer.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopUiScaleTest {

    @Test
    fun sanitizeKeepsSupportedOptions() {
        // 常规：全部合法档位原样返回
        assertEquals(80, DesktopUiScale.sanitize(80))
        assertEquals(100, DesktopUiScale.sanitize(100))
        assertEquals(150, DesktopUiScale.sanitize(150))
    }

    @Test
    fun sanitizeFallsBackToDefaultForUnsupportedValues() {
        // 边界：越界/未列入档位/负数一律回默认 100，避免界面被放大到不可用
        assertEquals(100, DesktopUiScale.sanitize(0))
        assertEquals(100, DesktopUiScale.sanitize(99))
        assertEquals(100, DesktopUiScale.sanitize(999))
        assertEquals(100, DesktopUiScale.sanitize(-50))
    }

    @Test
    fun factorConvertsPercentToDensityMultiplier() {
        assertEquals(1.0f, DesktopUiScale.factor(100))
        assertEquals(1.5f, DesktopUiScale.factor(150))
        assertEquals(0.8f, DesktopUiScale.factor(80))
        // 非法值经 sanitize 回默认后换算为 1.0
        assertEquals(1.0f, DesktopUiScale.factor(42))
    }

    @Test
    fun dpiInstallDeclaresAwarenessProperties() {
        // 启动层 DPI 感知：两个 JVM 属性须被置为 true（在任何 AWT 初始化前生效）
        DesktopDpi.install()
        assertEquals("true", System.getProperty(DesktopDpi.PROPERTY_DPI_AWARE))
        assertEquals("true", System.getProperty(DesktopDpi.PROPERTY_UI_SCALE_ENABLED))
    }
}
