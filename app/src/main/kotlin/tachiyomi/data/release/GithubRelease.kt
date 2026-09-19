package tachiyomi.data.release

import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.util.AppConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Contains information about the latest release from GitHub.
 */
@Serializable
data class GithubRelease(
    @SerialName("tag_name") val version: String,
    @SerialName("name") val name: String? = null,
    @SerialName("body") val info: String,
    @SerialName("html_url") val releaseLink: String,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("assets") val assets: List<GitHubAssets> = emptyList(),
) {
    fun getDownloadLink(): String {
        // 只能回落到 APK 资源：发布页同时挂着 mapping/符号表等文件，
        // 回落到 assets.firstOrNull() 会把它们当成安装包。
        val asset = getMatchedAsset()
        return asset?.browserDownloadUrl?.takeIf { it.isNotBlank() } ?: asset?.url.orEmpty()
    }

    fun getMatchedAsset(): GitHubAssets? {
        val apkAssets = assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
        return apkAssets.find { AppConfig.matchVariant(it.name) }
            ?: apkAssets.find { it.name.contains(BuildConfig.FLAVOR) && it.name.contains("universal", ignoreCase = true) }
            ?: apkAssets.find { it.name.contains(BuildConfig.FLAVOR) }
            ?: apkAssets.firstOrNull()
    }
}

/**
 * Assets class containing download url.
 */
@Serializable
data class GitHubAssets(
    val url: String,
    val name: String,
    val size: Long = 0L,
    @SerialName("browser_download_url") val browserDownloadUrl: String? = null,
)
