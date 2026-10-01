package com.ehviewer.desktop

import com.ehviewer.core.model.BaseGalleryInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopWindowManagerTest {

    @Test
    fun testWindowTitleForLibraryAndSettings() {
        assertEquals("EhViewer", DesktopWindowManager.windowTitle(DesktopWindowKind.Library))
        assertEquals("EhViewer Settings", DesktopWindowManager.windowTitle(DesktopWindowKind.Settings))
    }

    @Test
    fun testWindowTitleForGalleryDetail() {
        val galleryWithTitle = BaseGalleryInfo(gid = 1001, token = "tok1", title = "Touhou Project Manga")
        assertEquals(
            "Touhou Project Manga - EhViewer",
            DesktopWindowManager.windowTitle(DesktopWindowKind.GalleryDetail(galleryWithTitle)),
        )

        val galleryWithJpnTitle = BaseGalleryInfo(gid = 1002, token = "tok2", title = null, titleJpn = "東方Project")
        assertEquals(
            "東方Project - EhViewer",
            DesktopWindowManager.windowTitle(DesktopWindowKind.GalleryDetail(galleryWithJpnTitle)),
        )

        val galleryNoTitle = BaseGalleryInfo(gid = 1003, token = "tok3", title = null, titleJpn = null)
        assertEquals(
            "(Untitled) - EhViewer",
            DesktopWindowManager.windowTitle(DesktopWindowKind.GalleryDetail(galleryNoTitle)),
        )
    }

    @Test
    fun testOpenGalleryCreatesNewWindowWhenNotPresent() {
        val initialWindows = listOf(ShellWindow(id = 0, kind = DesktopWindowKind.Library))
        val gallery = BaseGalleryInfo(gid = 2001, token = "tokenA", title = "Gallery A")
        var nextId = 1L

        val (updatedWindows, activeId) = DesktopWindowManager.openOrFocusGallery(
            windows = initialWindows,
            gallery = gallery,
            nextIdProvider = { nextId++ },
        )

        assertEquals(2, updatedWindows.size)
        assertEquals(1L, activeId)
        val newWindow = updatedWindows.find { it.id == 1L }
        assertTrue(newWindow != null)
        val detailKind = newWindow.kind
        assertTrue(detailKind is DesktopWindowKind.GalleryDetail)
        assertEquals(2001L, detailKind.gallery.gid)
    }

    @Test
    fun testOpenGalleryReusesExistingWindowWhenAlreadyOpen() {
        val existingGallery = BaseGalleryInfo(gid = 3001, token = "tokenB", title = "Existing Gallery")
        val existingWindow = ShellWindow(id = 5, kind = DesktopWindowKind.GalleryDetail(existingGallery))
        val initialWindows = listOf(
            ShellWindow(id = 0, kind = DesktopWindowKind.Library),
            existingWindow,
        )
        var nextId = 10L

        // Attempt to open the exact same gallery again
        val (updatedWindows, activeId) = DesktopWindowManager.openOrFocusGallery(
            windows = initialWindows,
            gallery = BaseGalleryInfo(gid = 3001, token = "tokenB", title = "Existing Gallery New Instance"),
            nextIdProvider = { nextId++ },
        )

        assertEquals(2, updatedWindows.size, "Should not duplicate window for same gallery GID")
        assertEquals(5L, activeId, "Should return existing window id")
        assertEquals(10L, nextId, "Should not consume nextId when window is reused")
    }

    @Test
    fun testCloseWindowRemovesTarget() {
        val w1 = ShellWindow(id = 1, kind = DesktopWindowKind.Library)
        val w2 = ShellWindow(id = 2, kind = DesktopWindowKind.Settings)
        val list = listOf(w1, w2)

        val closed = DesktopWindowManager.closeWindow(list, id = 2)
        assertEquals(1, closed.size)
        assertEquals(1L, closed.first().id)

        val nonExistent = DesktopWindowManager.closeWindow(closed, id = 999)
        assertEquals(1, nonExistent.size)
    }
}
