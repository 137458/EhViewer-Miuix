package com.ehviewer.desktop

import com.ehviewer.core.database.client.GalleryMetadataParser
import com.ehviewer.core.model.BaseGalleryInfo

// Ctrl+O 快捷打开仅有 gid/token，开窗时是占位信息（无缩略图/分类/页数等真实元数据）。
// 该对象经官方 gdata API 拉取真实元数据回填，避免占位对象经收藏/历史路径把假数据写入数据库。
object DesktopGalleryHydrator {
    const val GDATA_URL = "https://api.e-hentai.org/api.php"

    fun gdataRequestBody(gid: Long, token: String): String = """{"method":"gdata","gidlist":[[$gid,"$token"]],"namespace":1}"""

    // 占位信息唯一可靠特征：从未接触真实元数据时 thumbKey 必为 null（gdata 命中必然回填缩略图键）
    fun needsHydration(info: BaseGalleryInfo): Boolean = info.thumbKey == null

    fun fetchInfo(
        gid: Long,
        token: String,
        fetch: (String, String) -> DesktopResponse = ::desktopPost,
    ): BaseGalleryInfo? {
        val response = runCatching { fetch(GDATA_URL, gdataRequestBody(gid, token)) }.getOrNull() ?: return null
        if (response.status !in 200..299) return null
        val info = BaseGalleryInfo(gid = gid, token = token)
        runCatching { GalleryMetadataParser.parse(response.body, listOf(info)) }.getOrNull() ?: return null
        // gmetadata 未命中（如无效 token）时占位字段不会被回填，视为拉取失败
        return if (needsHydration(info)) null else info
    }

    suspend fun voteComment(
        apiUid: Long,
        apiKey: String,
        gid: Long,
        token: String,
        commentId: Long,
        vote: Int,
        post: (String, String) -> DesktopResponse = ::desktopPost,
    ): Result<com.ehviewer.core.database.client.VoteCommentRequest.Result> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            val response = post(GDATA_URL, com.ehviewer.core.database.client.VoteCommentRequest.json(apiUid, apiKey, gid, token, commentId, vote))
            com.ehviewer.core.database.client.VoteCommentRequest.parseResult(response.body)
                ?: error("HTTP ${response.status}")
        }
    }
}
