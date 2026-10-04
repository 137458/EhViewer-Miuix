package com.ehviewer.desktop

import com.ehviewer.core.model.GalleryComment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopCommentsModelTest {
    private fun comment(id: Long, uploader: Boolean = false) = GalleryComment(
        id = id,
        score = 0,
        editable = false,
        voteUpAble = false,
        voteUpEd = false,
        voteDownAble = false,
        voteDownEd = false,
        uploader = uploader,
        voteState = null,
        time = 0L,
        user = "user$id",
        comment = "c$id",
        lastEdited = 0L,
    )

    @Test
    fun uploaderCommentsSortFirstWithStableOrder() {
        val a = comment(1)
        val uploader1 = comment(2, uploader = true)
        val b = comment(3)
        val uploader2 = comment(4, uploader = true)
        val sorted = DesktopCommentsModel.sortForDisplay(listOf(a, uploader1, b, uploader2))
        // 上传者评论置顶且组内保持原相对顺序，其余依次跟随
        assertEquals(listOf(2L, 4L, 1L, 3L), sorted.map { it.id })
    }

    @Test
    fun sortKeepsOrderWhenNoUploaderComments() {
        val sorted = DesktopCommentsModel.sortForDisplay(listOf(comment(7), comment(5), comment(6)))
        assertEquals(listOf(7L, 5L, 6L), sorted.map { it.id })
    }

    @Test
    fun collapsedShowsAtMostThreeComments() {
        val all = (1L..5L).map { comment(it) }
        assertEquals(3, DesktopCommentsModel.visibleCount(all, expanded = false))
        assertEquals(5, DesktopCommentsModel.visibleCount(all, expanded = true))
        // 不足三条时全显
        assertEquals(2, DesktopCommentsModel.visibleCount(all.take(2), expanded = false))
        assertEquals(0, DesktopCommentsModel.visibleCount(emptyList(), expanded = false))
    }

    @Test
    fun extractUrlsFindsHttpLinks() {
        assertEquals(
            listOf("https://e-hentai.org/g/1/2/"),
            DesktopCommentsModel.extractUrls("来源: https://e-hentai.org/g/1/2/ 看这个"),
        )
        assertEquals(
            listOf("http://a.example/x.jpg", "https://b.example/y"),
            DesktopCommentsModel.extractUrls("http://a.example/x.jpg 和 https://b.example/y"),
        )
    }

    @Test
    fun extractUrlsTrimsTrailingPunctuationAndDedupes() {
        // 句尾标点不属于 URL；重复链接去重保序
        assertEquals(
            listOf("https://e-hentai.org/g/1/2"),
            DesktopCommentsModel.extractUrls("见 https://e-hentai.org/g/1/2. 很好 https://e-hentai.org/g/1/2"),
        )
        assertEquals(emptyList(), DesktopCommentsModel.extractUrls("没有链接 ftp://x 不算 http:// 也空"))
    }

    @Test
    fun formatCommentTimeRendersDateAndTime() {
        // 固定时刻：2026-10-02 12:34 UTC → 平台时区渲染含日期与时间
        val epoch = 1791940440000L // 2026-10-02T12:34:00Z 前后（时区无关断言只查格式形状）
        val rendered = DesktopCommentsModel.formatCommentTime(epoch)
        assertTrue(rendered.isNotBlank())
        assertEquals(16, rendered.length)
        assertEquals('-', rendered[4])
        assertEquals(' ', rendered[10])
        assertEquals(':', rendered[13])
    }

    @Test
    fun formatScoreFormatsPositiveZeroAndNegative() {
        assertEquals("+5", DesktopCommentsModel.formatScore(5))
        assertEquals("+1", DesktopCommentsModel.formatScore(1))
        assertEquals("0", DesktopCommentsModel.formatScore(0))
        assertEquals("-3", DesktopCommentsModel.formatScore(-3))
        assertEquals("-10", DesktopCommentsModel.formatScore(-10))
    }

    @Test
    fun shouldDisplayScoreWhenVotingOrNonZero() {
        assertTrue(DesktopCommentsModel.shouldDisplayScore(score = 5, canVote = false))
        assertTrue(DesktopCommentsModel.shouldDisplayScore(score = -1, canVote = false))
        assertTrue(DesktopCommentsModel.shouldDisplayScore(score = 0, canVote = true))
        assertTrue(DesktopCommentsModel.shouldDisplayScore(score = 3, canVote = true))
        kotlin.test.assertFalse(DesktopCommentsModel.shouldDisplayScore(score = 0, canVote = false))
    }
}
