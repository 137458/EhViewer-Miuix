package com.ehviewer.desktop

import com.ehviewer.core.model.BaseGalleryInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopWindowManagerTest {

    @Test
    fun sessionSnapshotExtractsGalleryDetailWindowsInOrder() {
        val windows = listOf(
            ShellWindow(id = 0, kind = DesktopWindowKind.Library),
            ShellWindow(id = 1, kind = DesktopWindowKind.GalleryDetail(BaseGalleryInfo(gid = 100, token = "a", title = "A"))),
            ShellWindow(id = 2, kind = DesktopWindowKind.Settings),
            ShellWindow(id = 3, kind = DesktopWindowKind.GalleryDetail(BaseGalleryInfo(gid = 200, token = "b"))),
        )
        val snapshot = DesktopWindowManager.sessionSnapshot(windows)
        assertEquals(listOf(100L, 200L), snapshot.map { it.gid })
        assertEquals("a", snapshot[0].token)
        assertEquals("A", snapshot[0].title)
    }

    @Test
    fun sessionEncodeDecodeRoundTrips() {
        val snapshot = listOf(
            DesktopWindowManager.SessionGallery(gid = 100, token = "abc1234567", title = "A", titleJpn = "あ"),
            DesktopWindowManager.SessionGallery(gid = 200, token = "def7654321", title = null, titleJpn = null),
        )
        val encoded = DesktopWindowManager.encodeSession(snapshot)
        val decoded = DesktopWindowManager.decodeSession(encoded)
        assertEquals(snapshot, decoded)
        // 空列表往返
        assertEquals(emptyList(), DesktopWindowManager.decodeSession(DesktopWindowManager.encodeSession(emptyList())))
    }

    @Test
    fun sessionDecodeCorruptOrBlankReturnsEmpty() {
        assertEquals(emptyList(), DesktopWindowManager.decodeSession(null))
        assertEquals(emptyList(), DesktopWindowManager.decodeSession(""))
        assertEquals(emptyList(), DesktopWindowManager.decodeSession("not json at all"))
        assertEquals(emptyList(), DesktopWindowManager.decodeSession("""{"wrong":"shape"}"""))
    }

    @Test
    fun sessionDecodeCapsAtMaxRestoreWindows() {
        val many = (1L..15L).map { DesktopWindowManager.SessionGallery(gid = it, token = "t$it", title = null, titleJpn = null) }
        val decoded = DesktopWindowManager.decodeSession(DesktopWindowManager.encodeSession(many))
        assertEquals(DesktopWindowManager.MAX_RESTORE_WINDOWS, decoded.size)
        assertEquals(1L, decoded.first().gid)
    }

    @Test
    fun restoreWindowsBuildsGalleryDetailShellsPreservingOrder() {
        val saved = listOf(
            DesktopWindowManager.SessionGallery(gid = 300, token = "x", title = "X", titleJpn = null),
            DesktopWindowManager.SessionGallery(gid = 400, token = "y", title = null, titleJpn = "Y"),
        )
        var nextId = 7L
        val restored = DesktopWindowManager.restoreWindows(saved) { nextId++ }
        assertEquals(2, restored.size)
        assertEquals(7L, restored[0].id)
        assertEquals(8L, restored[1].id)
        val kind0 = restored[0].kind as DesktopWindowKind.GalleryDetail
        assertEquals(300L, kind0.gallery.gid)
        assertEquals("X", kind0.gallery.title)
        val kind1 = restored[1].kind as DesktopWindowKind.GalleryDetail
        assertEquals("y", kind1.gallery.token)
        assertEquals("Y", kind1.gallery.titleJpn)
    }

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

    @Test
    fun updateGalleryWindowReplacesGalleryPreservingIdentity() {
        val placeholder = BaseGalleryInfo(gid = 4001, token = "tok", title = "Gallery 4001")
        val hydrated = BaseGalleryInfo(gid = 4001, token = "tok", title = "Real Title", thumbKey = "a/b.jpg")
        val library = ShellWindow(id = 0, kind = DesktopWindowKind.Library)
        val detail = ShellWindow(id = 3, kind = DesktopWindowKind.GalleryDetail(placeholder))
        val list = listOf(library, detail)

        val updated = DesktopWindowManager.updateGalleryWindow(list, windowId = 3, gallery = hydrated)

        // 同 id 替换画廊信息，id 与位置不变（窗口身份稳定）
        assertEquals(2, updated.size)
        assertEquals(0L, updated[0].id)
        assertEquals(3L, updated[1].id)
        val kind = updated[1].kind as DesktopWindowKind.GalleryDetail
        assertEquals("Real Title", kind.gallery.title)
        assertEquals("a/b.jpg", kind.gallery.thumbKey)
        // 库窗口不受影响
        assertEquals(DesktopWindowKind.Library, updated[0].kind)

        // 窗口 id 不存在时原样返回
        assertEquals(list, DesktopWindowManager.updateGalleryWindow(list, windowId = 99, gallery = hydrated))
        // 替换非 GalleryDetail 窗口（如 Library）不生效
        assertEquals(list, DesktopWindowManager.updateGalleryWindow(list, windowId = 0, gallery = hydrated))
    }
}
