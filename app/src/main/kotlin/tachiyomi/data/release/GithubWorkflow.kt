package tachiyomi.data.release

import com.hippo.ehviewer.util.AppConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubRepo(
    @SerialName("default_branch") val defaultBranch: String,
)

@Serializable
data class GithubWorkflowRuns(
    @SerialName("workflow_runs") val workflowRuns: List<GithubWorkflowRun>,
)

@Serializable
data class GithubWorkflowRun(
    val id: Long = 0L,
    @SerialName("head_sha") val headSha: String,
    @SerialName("display_title") val title: String,
    @SerialName("artifacts_url") val artifactsUrl: String,
    @SerialName("html_url") val htmlUrl: String? = null,
)

@Serializable
data class GithubArtifacts(
    @SerialName("artifacts") val artifacts: List<GithubArtifact>,
) {
    fun getDownloadLink(): String {
        // The default order is upload order, so we need to sort it
        return artifacts.sortedBy { it.name }.run {
            // CI also publishes mapping and native-symbol archives. Restrict the
            // fallback to ABI build artifacts so an unsupported ABI cannot download
            // a mapping file archive as an update package.
            val apkArtifacts = filter { artifact ->
                ABI_MARKERS.any { marker -> artifact.name.contains(marker, ignoreCase = true) }
            }
            apkArtifacts.find { AppConfig.matchVariant(it.name) } ?: apkArtifacts.firstOrNull()
        }?.downloadLink.orEmpty()
    }
}

private val ABI_MARKERS = arrayOf("universal", "arm64-v8a", "armeabi-v7a", "x86_64")

@Serializable
data class GithubArtifact(
    val name: String,
    @SerialName("archive_download_url") val downloadLink: String,
)
