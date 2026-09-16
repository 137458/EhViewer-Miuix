package tachiyomi.data.release

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
        val asset = assets.find { AppConfig.matchVariant(it.name) } ?: assets[0]
        return asset.url
    }

    fun getMatchedAsset(): GitHubAssets? {
        return assets.find { AppConfig.matchVariant(it.name) } ?: assets.firstOrNull()
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
