package com.ehviewer.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopNotificationStateTest {

    @Test
    fun testPostNotificationAddsItem() {
        var idCounter = 1L
        val initial = emptyList<DesktopNotification>()
        val result = DesktopNotificationManager.post(
            current = initial,
            message = "Copied to clipboard",
            timestamp = 1000L,
            maxKeep = 3,
            idProvider = { idCounter++ },
        )

        assertEquals(1, result.size)
        val first = result.first()
        assertEquals(1L, first.id)
        assertEquals("Copied to clipboard", first.message)
        assertEquals(1000L, first.timestamp)
    }

    @Test
    fun testPostNotificationTrimsToMaxKeep() {
        var idCounter = 1L
        var list = emptyList<DesktopNotification>()

        for (i in 1..5) {
            list = DesktopNotificationManager.post(
                current = list,
                message = "Notice $i",
                timestamp = i * 1000L,
                maxKeep = 3,
                idProvider = { idCounter++ },
            )
        }

        assertEquals(3, list.size, "Should keep at most maxKeep items")
        assertEquals(listOf(3L, 4L, 5L), list.map { it.id }, "Should keep newest items")
        assertEquals(listOf("Notice 3", "Notice 4", "Notice 5"), list.map { it.message })
    }

    @Test
    fun testDismissNotification() {
        val list = listOf(
            DesktopNotification(id = 1L, message = "First", timestamp = 1000L),
            DesktopNotification(id = 2L, message = "Second", timestamp = 2000L),
            DesktopNotification(id = 3L, message = "Third", timestamp = 3000L),
        )

        val updated = DesktopNotificationManager.dismiss(list, id = 2L)
        assertEquals(2, updated.size)
        assertEquals(listOf(1L, 3L), updated.map { it.id })

        val unchanged = DesktopNotificationManager.dismiss(updated, id = 999L)
        assertEquals(2, unchanged.size)
    }

    @Test
    fun testExpireNotifications() {
        val list = listOf(
            DesktopNotification(id = 1L, message = "Old", timestamp = 1000L),
            DesktopNotification(id = 2L, message = "Middle", timestamp = 2500L),
            DesktopNotification(id = 3L, message = "Fresh", timestamp = 3500L),
        )

        // At currentTime = 4000L with ttlMs = 2000L (items before 2000L expire)
        val active = DesktopNotificationManager.expire(list, currentTime = 4000L, ttlMs = 2000L)
        assertEquals(2, active.size)
        assertEquals(listOf(2L, 3L), active.map { it.id })

        // At currentTime = 6000L with ttlMs = 2000L (all expire)
        val allExpired = DesktopNotificationManager.expire(list, currentTime = 6000L, ttlMs = 2000L)
        assertTrue(allExpired.isEmpty())
    }

    @Test
    fun postPreservesUndoAction() {
        var undoFired = false
        val list = DesktopNotificationManager.post(
            current = emptyList(),
            message = "removed",
            timestamp = 1000L,
            idProvider = { 7L },
            actionLabel = "Undo",
            onAction = { undoFired = true },
        )
        val notice = list.single()
        assertEquals("Undo", notice.actionLabel)
        // expire 不破坏 action 字段与回调
        val survived = DesktopNotificationManager.expire(list, currentTime = 2000L, ttlMs = 2500L)
        assertEquals("Undo", survived.single().actionLabel)
        survived.single().onAction?.invoke()
        assertEquals(true, undoFired)
    }
}
