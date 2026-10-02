package com.ehviewer.desktop

import com.ehviewer.core.model.GalleryComment
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
