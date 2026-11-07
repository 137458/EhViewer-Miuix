package com.hippo.ehviewer.ui.update

import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.updater.Release
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdateDialogPlanTest {
    private fun release(
        version: String = "1.2.3",
        changelog: String = "- note",
        downloadLink: String = "https://example.invalid/app.apk",
        releaseUrl: String = "https://example.invalid/releases/tag/1.2.3",
        publishedAt: String = "2026-03-04",
        isCI: Boolean = false,
    ) = Release(
        version = version,
        changelog = changelog,
        downloadLink = downloadLink,
        releaseUrl = releaseUrl,
        publishedAt = publishedAt,
        isCI = isCI,
        hasUpdate = true,
    )

    @Test
    fun `summary carries current latest and date`() {
        val summary = updateDialogSummary("1.1.0", release())

        assertEquals("1.1.0", summary.currentVersion)
        assertEquals("1.2.3", summary.latestVersion)
        assertEquals("2026-03-04", summary.publishedAt)
    }

    @Test
    fun `summary omits missing date`() {
        val summary = updateDialogSummary("1.1.0", release(publishedAt = ""))

        assertEquals("", summary.publishedAt)
    }

    @Test
    fun `idle offers download later and ignore link`() {
        val plan = buildUpdateDialogPlan(
            currentVersion = "1.1.0",
            release = release(),
            download = UpdateDownloadSnapshot(),
            canIgnore = true,
            isSimulated = false,
        )

        assertEquals(UpdatePrimaryAction.Download, plan.primary)
        assertEquals(UpdateDismissAction.Later, plan.dismiss)
        assertFalse(plan.showCancelDownload)
        assertFalse(plan.showBrowserFallback)
        assertTrue(plan.showIgnore)
        assertFalse(plan.showProgress)
        assertNull(plan.progressFraction)
        assertNull(plan.progress)
        assertTrue(plan.hasNotes)
        assertEquals(UpdateChannel.Official, plan.channel)
    }

    @Test
    fun `downloading swaps to progress cancel and background`() {
        val snapshot = UpdateDownloadSnapshot(
            isDownloading = true,
            progress = 0.87f,
            downloadedBytes = 8_700L,
            totalBytes = 10_000L,
            speedBytesPerSec = 1_024L,
        )

        val plan = buildUpdateDialogPlan("1.1.0", release(), snapshot, canIgnore = true, isSimulated = false)

        assertEquals(UpdatePrimaryAction.Downloading, plan.primary)
        assertEquals(87, plan.downloadPercent)
        assertEquals(UpdateDismissAction.Background, plan.dismiss)
        assertTrue(plan.showCancelDownload)
        assertTrue(plan.showProgress)
        assertFalse(plan.showIgnore)
        assertFalse(plan.showBrowserFallback)
        assertEquals(0.87f, plan.progressFraction)
        assertEquals(UpdateProgressNumbers(8_700L, 10_000L, 1_024L), plan.progress)
    }

    @Test
    fun `failure offers retry and direct browser fallback`() {
        val snapshot = UpdateDownloadSnapshot(error = "network down")

        val plan = buildUpdateDialogPlan("1.1.0", release(isCI = true), snapshot, canIgnore = true, isSimulated = false)

        assertEquals(UpdatePrimaryAction.Retry, plan.primary)
        assertEquals(UpdateDismissAction.Later, plan.dismiss)
        assertTrue(plan.showBrowserFallback)
        assertFalse(plan.showCancelDownload)
        assertFalse(plan.showProgress)
        assertTrue(plan.showIgnore)
        assertEquals(UpdateChannel.Ci, plan.channel)
    }

    @Test
    fun `downloaded package offers install`() {
        val snapshot = UpdateDownloadSnapshot(doneFile = File("update.apk"))

        val plan = buildUpdateDialogPlan("1.1.0", release(), snapshot, canIgnore = true, isSimulated = false)

        assertEquals(UpdatePrimaryAction.Install, plan.primary)
        assertEquals(UpdateDismissAction.Later, plan.dismiss)
        assertFalse(plan.showBrowserFallback)
        assertFalse(plan.showProgress)
    }

    @Test
    fun `missing direct link falls back to release page`() {
        val plan = buildUpdateDialogPlan(
            "1.1.0",
            release(downloadLink = ""),
            UpdateDownloadSnapshot(),
            canIgnore = false,
            isSimulated = false,
        )

        assertEquals(UpdatePrimaryAction.OpenRelease, plan.primary)
    }

    @Test
    fun `blank changelog means no notes`() {
        val plan = buildUpdateDialogPlan(
            "1.1.0",
            release(changelog = "  "),
            UpdateDownloadSnapshot(),
            canIgnore = false,
            isSimulated = false,
        )

        assertFalse(plan.hasNotes)
    }

    @Test
    fun `simulated channel wins over ci`() {
        val plan = buildUpdateDialogPlan(
            "1.1.0",
            release(isCI = true),
            UpdateDownloadSnapshot(),
            canIgnore = false,
            isSimulated = true,
        )

        assertEquals(UpdateChannel.Simulated, plan.channel)
    }

    @Test
    fun `notes collapse to twelve lines and report hidden count`() {
        val markdown = (1..20).joinToString("\n") { "line $it" }

        val state = updateDialogNotesState(markdown, expanded = false)

        assertEquals(8, state.hiddenLineCount)
        assertEquals(13, state.visibleMarkdown.lines().size)
        assertTrue(state.visibleMarkdown.startsWith("line 1\n"))
        assertTrue(state.visibleMarkdown.endsWith("…"))
    }

    @Test
    fun `expanded notes are untouched`() {
        val markdown = (1..20).joinToString("\n") { "line $it" }

        val state = updateDialogNotesState(markdown, expanded = true)

        assertEquals(0, state.hiddenLineCount)
        assertEquals(markdown, state.visibleMarkdown)
    }

    @Test
    fun `short notes are untouched`() {
        val markdown = "line 1\nline 2"

        val state = updateDialogNotesState(markdown, expanded = false)

        assertEquals(0, state.hiddenLineCount)
        assertEquals(markdown, state.visibleMarkdown)
    }

    @Test
    fun `download percent floors and clamps`() {
        assertEquals(87, updateDownloadPercent(0.87f))
        assertEquals(33, updateDownloadPercent(1f / 3f))
        assertEquals(100, updateDownloadPercent(1.2f))
        assertEquals(0, updateDownloadPercent(-0.1f))
    }

    @Test
    fun `progress fraction needs a known total`() {
        assertEquals(
            0.3f,
            updateDialogProgressFraction(UpdateDownloadSnapshot(downloadedBytes = 30L, totalBytes = 100L)),
        )
        assertNull(updateDialogProgressFraction(UpdateDownloadSnapshot(downloadedBytes = 30L, totalBytes = 0L)))
        assertEquals(
            1f,
            updateDialogProgressFraction(UpdateDownloadSnapshot(downloadedBytes = 120L, totalBytes = 100L)),
        )
    }

    @Test
    fun `browser fallback prefers the direct apk link`() {
        assertEquals(
            "https://example.invalid/direct.apk",
            updateBrowserFallbackUrl(
                release(
                    downloadLink = "https://example.invalid/direct.apk",
                    releaseUrl = "https://example.invalid/releases/tag/1.2.3",
                ),
            ),
        )
        assertEquals(
            "https://example.invalid/releases/tag/1.2.3",
            updateBrowserFallbackUrl(release(downloadLink = "")),
        )
        assertEquals(
            "https://github.com/${BuildConfig.REPO_NAME}/releases",
            updateBrowserFallbackUrl(release(downloadLink = "", releaseUrl = "")),
        )
    }

    @Test
    fun `ignore and prompted suppress automatic prompts but never user action`() {
        assertTrue(shouldPromptUpdate("1.2.3", ignoredVersion = null, promptedVersion = null, userInitiated = false))
        assertFalse(shouldPromptUpdate("1.2.3", ignoredVersion = "1.2.3", promptedVersion = null, userInitiated = false))
        assertFalse(shouldPromptUpdate("1.2.3", ignoredVersion = null, promptedVersion = "1.2.3", userInitiated = false))
        assertTrue(shouldPromptUpdate("1.2.3", ignoredVersion = "1.2.2", promptedVersion = null, userInitiated = false))
        assertTrue(
            shouldPromptUpdate(
                "1.2.3",
                ignoredVersion = "1.2.3",
                promptedVersion = "1.2.3",
                userInitiated = true,
            ),
        )
    }
}
