package com.ehviewer.desktop

import com.ehviewer.core.model.BaseGalleryInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

// 单窗口页面栈语义：库为根，详情/阅读/设置逐层推入，Esc 逐层返回；会话快照只存详情链
class DesktopPageStackTest {

    private val g1 = BaseGalleryInfo(gid = 100L, token = "tokena", title = "Gallery A")
    private val g2 = BaseGalleryInfo(gid = 200L, token = "tokenb", title = "Gallery B")
    private val g1Hydrated = BaseGalleryInfo(gid = 100L, token = "tokena", title = "Real Title", thumbKey = "a/b.jpg")

    @Test
    fun initialStackIsSingleLibraryPage() {
        val stack = DesktopPageStack.initial()
        assertEquals(listOf<DesktopPage>(DesktopPage.Library), stack)
    }

    @Test
    fun pushAppendsPageOnTop() {
        val stack = DesktopPageStack.push(DesktopPageStack.initial(), DesktopPage.GalleryDetail(g1))
        assertEquals(2, stack.size)
        assertIs<DesktopPage.GalleryDetail>(stack.last())

        // 继续推入阅读页：详情 → 阅读两层
        val withReader = DesktopPageStack.push(stack, DesktopPage.Reader(g1))
        assertEquals(3, withReader.size)
        assertIs<DesktopPage.Reader>(withReader.last())
        // 原栈不受影响（纯函数）
        assertEquals(2, stack.size)
    }

    @Test
    fun pushSameTopPageIsNoOp() {
        // 双击/重复触发防抖：栈顶同画廊详情不重复推入
        val stack = DesktopPageStack.push(DesktopPageStack.initial(), DesktopPage.GalleryDetail(g1))
        val again = DesktopPageStack.push(stack, DesktopPage.GalleryDetail(g1))
        assertEquals(stack, again)

        val reader = DesktopPageStack.push(stack, DesktopPage.Reader(g1))
        assertEquals(reader, DesktopPageStack.push(reader, DesktopPage.Reader(g1)))
    }

    @Test
    fun pushDifferentGalleryAppends() {
        val stack = DesktopPageStack.push(DesktopPageStack.initial(), DesktopPage.GalleryDetail(g1))
        val two = DesktopPageStack.push(stack, DesktopPage.GalleryDetail(g2))
        assertEquals(3, two.size)
        assertEquals(g2.gid, (two.last() as DesktopPage.GalleryDetail).gallery.gid)
    }

    @Test
    fun popRemovesTopPage() {
        val stack = DesktopPageStack.push(
            DesktopPageStack.push(DesktopPageStack.initial(), DesktopPage.GalleryDetail(g1)),
            DesktopPage.Reader(g1),
        )
        val back = DesktopPageStack.pop(stack)
        assertEquals(2, back.size)
        assertIs<DesktopPage.GalleryDetail>(back.last())
    }

    @Test
    fun popAtRootKeepsLibrary() {
        // 根保护：库页永不出栈，pop 不得产生空栈
        val root = DesktopPageStack.initial()
        assertSame(root, DesktopPageStack.pop(root))

        val empty = DesktopPageStack.pop(emptyList())
        assertEquals(listOf<DesktopPage>(DesktopPage.Library), empty)
    }

    @Test
    fun popToRootReturnsLibraryOnly() {
        val deep = DesktopPageStack.push(
            DesktopPageStack.push(
                DesktopPageStack.push(DesktopPageStack.initial(), DesktopPage.Settings),
                DesktopPage.GalleryDetail(g1),
            ),
            DesktopPage.Reader(g1),
        )
        assertEquals(listOf<DesktopPage>(DesktopPage.Library), DesktopPageStack.popToRoot(deep))
        // 已在根：原样
        val root = DesktopPageStack.initial()
        assertSame(root, DesktopPageStack.popToRoot(root))
    }

    @Test
    fun updateTopGalleryReplacesDetailGalleryInPlace() {
        // hydrate 回写：栈顶详情页的占位画廊被真实元数据整体替换，栈结构不变
        val stack = DesktopPageStack.push(DesktopPageStack.initial(), DesktopPage.GalleryDetail(g1))
        val updated = DesktopPageStack.updateTopGallery(stack, g1Hydrated)
        assertEquals(2, updated.size)
        val top = updated.last() as DesktopPage.GalleryDetail
        assertEquals("Real Title", top.gallery.title)
        assertEquals("a/b.jpg", top.gallery.thumbKey)
        // 原栈不被修改
        assertNotEquals(updated, stack)
    }

    @Test
    fun updateTopGalleryOnNonDetailTopIsNoOp() {
        val stack = DesktopPageStack.push(DesktopPageStack.initial(), DesktopPage.Reader(g1))
        assertEquals(stack, DesktopPageStack.updateTopGallery(stack, g1Hydrated))
        assertEquals(
            DesktopPageStack.initial(),
            DesktopPageStack.updateTopGallery(DesktopPageStack.initial(), g1Hydrated),
        )
    }

    @Test
    fun detailSnapshotExtractsDetailsInStackOrder() {
        val stack = listOf<DesktopPage>(
            DesktopPage.Library,
            DesktopPage.Settings,
            DesktopPage.GalleryDetail(g1),
            DesktopPage.Reader(g1),
            DesktopPage.GalleryDetail(g2),
        )
        val snapshot = DesktopPageStack.detailSnapshot(stack)
        assertEquals(listOf(100L, 200L), snapshot.map { it.gid })
        assertEquals("tokena", snapshot[0].token)
        assertEquals("Gallery A", snapshot[0].title)
    }

    @Test
    fun sessionEncodeDecodeRoundTrips() {
        val snapshot = listOf(
            DesktopPageStack.SessionGallery(gid = 100, token = "abc1234567", title = "A", titleJpn = "あ"),
            DesktopPageStack.SessionGallery(gid = 200, token = "def7654321", title = null, titleJpn = null),
        )
        val encoded = DesktopPageStack.encodeSession(snapshot)
        assertEquals(snapshot, DesktopPageStack.decodeSession(encoded))
        assertEquals(emptyList(), DesktopPageStack.decodeSession(DesktopPageStack.encodeSession(emptyList())))
    }

    @Test
    fun sessionDecodeCorruptOrBlankReturnsEmpty() {
        assertEquals(emptyList(), DesktopPageStack.decodeSession(null))
        assertEquals(emptyList(), DesktopPageStack.decodeSession(""))
        assertEquals(emptyList(), DesktopPageStack.decodeSession("not json at all"))
        assertEquals(emptyList(), DesktopPageStack.decodeSession("""{"wrong":"shape"}"""))
    }

    @Test
    fun sessionDecodeHonorsLimit() {
        val many = (1L..15L).map { DesktopPageStack.SessionGallery(gid = it, token = "t$it", title = null, titleJpn = null) }
        val encoded = DesktopPageStack.encodeSession(many)
        assertEquals(DesktopPageStack.MAX_RESTORE_WINDOWS, DesktopPageStack.decodeSession(encoded).size)
        assertEquals(3, DesktopPageStack.decodeSession(encoded, limit = 3).size)
        assertEquals(15, DesktopPageStack.decodeSession(encoded, limit = 20).size)
    }

    @Test
    fun restorePagesBuildsLibraryPlusDetailChain() {
        val saved = listOf(
            DesktopPageStack.SessionGallery(gid = 300, token = "x", title = "X", titleJpn = null),
            DesktopPageStack.SessionGallery(gid = 400, token = "y", title = null, titleJpn = "Y"),
        )
        val pages = DesktopPageStack.restorePages(saved)
        assertEquals(3, pages.size)
        assertEquals(DesktopPage.Library, pages.first())
        val top = pages.last() as DesktopPage.GalleryDetail
        assertEquals(400L, top.gallery.gid)
        assertEquals("Y", top.gallery.titleJpn)
    }

    @Test
    fun restorePagesEmptyGivesLibraryOnly() {
        assertEquals(listOf<DesktopPage>(DesktopPage.Library), DesktopPageStack.restorePages(emptyList()))
    }

    @Test
    fun sanitizeRestoreLimitClampsToSaneRange() {
        assertEquals(10, DesktopPageStack.sanitizeRestoreLimit(0))
        assertEquals(10, DesktopPageStack.sanitizeRestoreLimit(-5))
        assertEquals(10, DesktopPageStack.sanitizeRestoreLimit(21))
        assertEquals(5, DesktopPageStack.sanitizeRestoreLimit(5))
        assertEquals(20, DesktopPageStack.sanitizeRestoreLimit(20))
    }

    @Test
    fun pageTitleFollowsTopPage() {
        val untitled = "(Untitled)"
        val settingsLabel = "Settings"
        val readingLabel = "Reading"
        val navItemsLabel = "Nav Items"
        assertEquals("EhViewer", DesktopPageStack.pageTitle(DesktopPage.Library, untitled, settingsLabel, readingLabel, navItemsLabel))
        assertEquals("EhViewer Settings", DesktopPageStack.pageTitle(DesktopPage.Settings, untitled, settingsLabel, readingLabel, navItemsLabel))
        // 导航项子页标题跟随本地化标签
        assertEquals("EhViewer Nav Items", DesktopPageStack.pageTitle(DesktopPage.NavItems, untitled, settingsLabel, readingLabel, navItemsLabel))
        assertEquals(
            "Gallery A - EhViewer",
            DesktopPageStack.pageTitle(DesktopPage.GalleryDetail(g1), untitled, settingsLabel, readingLabel, navItemsLabel),
        )
        // 无标题详情回退 untitled 标签
        val noTitle = BaseGalleryInfo(gid = 1003, token = "t3")
        assertEquals(
            "(Untitled) - EhViewer",
            DesktopPageStack.pageTitle(DesktopPage.GalleryDetail(noTitle), untitled, settingsLabel, readingLabel, navItemsLabel),
        )
        assertEquals(
            "Gallery A - Reading",
            DesktopPageStack.pageTitle(DesktopPage.Reader(g1), untitled, settingsLabel, readingLabel, navItemsLabel),
        )
        // 无标题阅读页同样回退 untitled 标签（不再回退 GID）
        assertEquals(
            "(Untitled) - Reading",
            DesktopPageStack.pageTitle(DesktopPage.Reader(noTitle), untitled, settingsLabel, readingLabel, navItemsLabel),
        )
    }
}
