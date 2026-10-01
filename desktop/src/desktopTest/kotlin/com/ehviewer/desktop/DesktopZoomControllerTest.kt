package com.ehviewer.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopZoomControllerTest {

    @Test
    fun zoomIn_increasesScale() {
        val result = DesktopZoomController.zoomIn(1.0f)
        assertEquals(1.25f, result)
    }

    @Test
    fun zoomIn_withCustomStep_increasesProperly() {
        val result = DesktopZoomController.zoomIn(1.0f, step = 0.5f)
        assertEquals(1.5f, result)
    }

    @Test
    fun zoomIn_clampsAtMaxScale() {
        val result = DesktopZoomController.zoomIn(3.9f)
        assertEquals(4.0f, result)
        val overflow = DesktopZoomController.zoomIn(4.0f)
        assertEquals(4.0f, overflow)
    }

    @Test
    fun zoomOut_decreasesScale() {
        val result = DesktopZoomController.zoomOut(1.0f)
        assertEquals(0.75f, result)
    }

    @Test
    fun zoomOut_withCustomStep_decreasesProperly() {
        val result = DesktopZoomController.zoomOut(1.0f, step = 0.5f)
        assertEquals(0.5f, result)
    }

    @Test
    fun zoomOut_clampsAtMinScale() {
        val result = DesktopZoomController.zoomOut(0.6f)
        assertEquals(0.5f, result)
        val underflow = DesktopZoomController.zoomOut(0.5f)
        assertEquals(0.5f, underflow)
    }

    @Test
    fun zoomInAndOut_preservesPrecisionWithoutFloatingDrift() {
        var scale = 1.0f
        repeat(4) { scale = DesktopZoomController.zoomIn(scale, step = 0.25f) }
        assertEquals(2.0f, scale)
        repeat(4) { scale = DesktopZoomController.zoomOut(scale, step = 0.25f) }
        assertEquals(1.0f, scale)
    }

    @Test
    fun resetZoom_returnsOne() {
        assertEquals(1.0f, DesktopZoomController.resetZoom())
    }

    @Test
    fun toggleFitZoom_togglesBetweenFitAndZoomed() {
        val zoomed = DesktopZoomController.toggleFitZoom(1.0f)
        assertEquals(2.0f, zoomed)
        val reset = DesktopZoomController.toggleFitZoom(2.0f)
        assertEquals(1.0f, reset)
        val fromArbitrary = DesktopZoomController.toggleFitZoom(1.75f)
        assertEquals(1.0f, fromArbitrary)
    }

    @Test
    fun formatZoomPercentage_formatsProperly() {
        assertEquals("100%", DesktopZoomController.formatZoomPercentage(1.0f))
        assertEquals("150%", DesktopZoomController.formatZoomPercentage(1.5f))
        assertEquals("50%", DesktopZoomController.formatZoomPercentage(0.5f))
        assertEquals("75%", DesktopZoomController.formatZoomPercentage(0.75f))
        assertEquals("0%", DesktopZoomController.formatZoomPercentage(0.0f))
        assertEquals("333%", DesktopZoomController.formatZoomPercentage(3.333f))
    }
}
