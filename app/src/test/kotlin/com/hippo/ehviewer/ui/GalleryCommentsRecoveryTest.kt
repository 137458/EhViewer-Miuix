package com.hippo.ehviewer.ui

import com.ehviewer.core.model.GalleryCommentList
import com.ehviewer.core.model.GalleryDetail
import com.hippo.ehviewer.ui.screen.GalleryCommentsState
import com.hippo.ehviewer.ui.screen.resolveGalleryCommentsState
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GalleryCommentsRecoveryTest {

    @Test
    fun `when detail cache hits, state is Ready`() {
        val dummyDetail = GalleryDetail(
            tagGroups = emptyList(),
            comments = GalleryCommentList(emptyList(), false),
            previewList = emptyList(),
        )
        val state = resolveGalleryCommentsState(
            gid = 12345L,
            token = "token123",
            cachedDetail = dummyDetail,
        )

        val readyState = assertIs<GalleryCommentsState.Ready>(state)
        assertEquals(dummyDetail, readyState.galleryDetail)
    }

    @Test
    fun `when detail cache misses but token is available, state is NeedsFetch and no NPE thrown`() {
        val state = resolveGalleryCommentsState(
            gid = 12345L,
            token = "token123",
            cachedDetail = null,
        )

        val fetchState = assertIs<GalleryCommentsState.NeedsFetch>(state)
        assertEquals(12345L, fetchState.gid)
        assertEquals("token123", fetchState.token)
    }

    @Test
    fun `when detail cache misses and token is null, state is MissingDetail safely`() {
        val state = resolveGalleryCommentsState(
            gid = 12345L,
            token = null,
            cachedDetail = null,
        )

        assertIs<GalleryCommentsState.MissingDetail>(state)
    }
}
