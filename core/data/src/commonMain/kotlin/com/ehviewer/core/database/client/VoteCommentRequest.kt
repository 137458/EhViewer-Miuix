package com.ehviewer.core.database.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// 评论投票 API 请求体构造与响应解析（与 app EhEngine.voteComment 的字段一一对应）：
// POST api 公共端点，JSON: {method: votecomment, apiuid, apikey, gid, token, comment_id, comment_vote}
object VoteCommentRequest {
    private val json = Json { ignoreUnknownKeys = true }

    fun json(
        apiUid: Long,
        apiKey: String,
        gid: Long,
        token: String,
        commentId: Long,
        vote: Int,
    ): String = json.encodeToString(
        Body(
            method = "votecomment",
            apiuid = apiUid,
            apikey = apiKey,
            gid = gid,
            token = token,
            commentId = commentId,
            commentVote = vote,
        ),
    )

    @Serializable
    private data class Body(
        val method: String,
        val apiuid: Long,
        val apikey: String,
        val gid: Long,
        val token: String,
        @SerialName("comment_id") val commentId: Long,
        @SerialName("comment_vote") val commentVote: Int,
    )

    @Serializable
    data class Result(
        @SerialName("comment_id") val id: Long,
        @SerialName("comment_score") val score: Int,
        @SerialName("comment_vote") val vote: Int,
    )

    fun parseResult(body: String): Result? = runCatching { json.decodeFromString<Result>(body) }.getOrNull()
}
