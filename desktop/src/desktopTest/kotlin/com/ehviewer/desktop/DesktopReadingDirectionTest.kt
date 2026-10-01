package com.ehviewer.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopReadingDirectionTest {

    @Test
    fun pageDeltaForZonesFollowsDirection() {
        // LTR：右区下一页（+1），左区上一页（-1）
        assertEquals(1, DesktopReadingDirection.LTR.pageDeltaForZone(rightZone = true))
        assertEquals(-1, DesktopReadingDirection.LTR.pageDeltaForZone(rightZone = false))
        // RTL：日漫从右往左——右区上一页（-1），左区下一页（+1）
        assertEquals(-1, DesktopReadingDirection.RTL.pageDeltaForZone(rightZone = true))
        assertEquals(1, DesktopReadingDirection.RTL.pageDeltaForZone(rightZone = false))
    }

    @Test
    fun pageDeltaForKeyFollowsDirection() {
        // LTR：→ 下一页，← 上一页；RTL 反转
        assertEquals(1, DesktopReadingDirection.LTR.pageDeltaForKey(forward = true))
        assertEquals(-1, DesktopReadingDirection.LTR.pageDeltaForKey(forward = false))
        assertEquals(-1, DesktopReadingDirection.RTL.pageDeltaForKey(forward = true))
        assertEquals(1, DesktopReadingDirection.RTL.pageDeltaForKey(forward = false))
    }

    @Test
    fun fromPersistedValueParsesAndFallsBack() {
        assertEquals(DesktopReadingDirection.LTR, DesktopReadingDirection.fromPersisted("LTR"))
        assertEquals(DesktopReadingDirection.RTL, DesktopReadingDirection.fromPersisted("RTL"))
        // 非法/空值回退 LTR（西式阅读为默认）
        assertEquals(DesktopReadingDirection.LTR, DesktopReadingDirection.fromPersisted(null))
        assertEquals(DesktopReadingDirection.LTR, DesktopReadingDirection.fromPersisted(""))
        assertEquals(DesktopReadingDirection.LTR, DesktopReadingDirection.fromPersisted("garbage"))
    }

    @Test
    fun toggleAlternatesDirection() {
        assertEquals(DesktopReadingDirection.RTL, DesktopReadingDirection.LTR.toggle())
        assertEquals(DesktopReadingDirection.LTR, DesktopReadingDirection.RTL.toggle())
    }
}
