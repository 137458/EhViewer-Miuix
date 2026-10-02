package com.ehviewer.core.database.client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VoteCommentRequestTest {
    @Test
    fun jsonContainsAllApiFieldsInShape() {
        val body = VoteCommentRequest.json(
            apiUid = 42L,
            apiKey = "abc123",
            gid = 618395L,
            token = "0439fa3666",
            commentId = 987654L,
            vote = 1,
        )
        // 字段名与 app EhEngine.voteComment 逐一对应
        assertTrue(body.contains("\"method\":\"votecomment\""))
        assertTrue(body.contains("\"apiuid\":42"))
        assertTrue(body.contains("\"apikey\":\"abc123\""))
        assertTrue(body.contains("\"gid\":618395"))
        assertTrue(body.contains("\"token\":\"0439fa3666\""))
        assertTrue(body.contains("\"comment_id\":987654"))
        assertTrue(body.contains("\"comment_vote\":1"))
    }

    @Test
    fun negativeVoteSerializes() {
        val body = VoteCommentRequest.json(1L, "k", 2L, "t", 3L, vote = -1)
        assertTrue(body.contains("\"comment_vote\":-1"))
    }

    @Test
    fun parseResultReadsSnakeCaseResponse() {
        val result = VoteCommentRequest.parseResult(
            """{"comment_id":987654,"comment_score":12,"comment_vote":1}""",
        )
        assertEquals(987654L, result!!.id)
        assertEquals(12, result.score)
        assertEquals(1, result.vote)
    }

    @Test
    fun parseResultReturnsNullOnNonJsonBody() {
        // 站点错误页/空体等异常响应安全返回 null
        assertNull(VoteCommentRequest.parseResult("<html>error</html>"))
        assertNull(VoteCommentRequest.parseResult(""))
    }
}
