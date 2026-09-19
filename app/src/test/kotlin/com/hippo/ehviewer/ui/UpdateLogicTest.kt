package com.hippo.ehviewer.ui

import com.hippo.ehviewer.updater.AppUpdater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateLogicTest {

    @Test
    fun testVersionComparison() {
        // Standard semantic versions
        assertTrue(AppUpdater.compareVersions("v2.1.0", "v2.0.9") > 0)
        assertTrue(AppUpdater.compareVersions("2.0.9", "2.1.0") < 0)
        assertEquals(0, AppUpdater.compareVersions("v2.1.0", "2.1.0"))
        assertEquals(0, AppUpdater.compareVersions("2.0.0", "2.0.0"))

        // Patch version comparison
        assertTrue(AppUpdater.compareVersions("v2.0.1", "v2.0.0") > 0)
        assertTrue(AppUpdater.compareVersions("v2.0.10", "v2.0.9") > 0)

        // Pre-release versions
        assertTrue(AppUpdater.compareVersions("2.1.0-beta02", "2.1.0-beta01") > 0)
        assertTrue(AppUpdater.compareVersions("2.1.0", "2.1.0-beta01") > 0)

        // Fallback string comparison
        assertTrue(AppUpdater.compareVersions("b_release", "a_release") > 0)
    }

    @Test
    fun testShouldUpdate() {
        // Local is newer than remote (e.g. nightly or dev build) -> should NOT update
        assertTrue(AppUpdater.compareVersions("v2.1.0", "v2.2.0") < 0)
        // Local is older than remote -> should update
        assertTrue(AppUpdater.compareVersions("v2.2.0", "v2.1.0") > 0)
        // Equal versions -> should NOT update
        assertEquals(0, AppUpdater.compareVersions("v2.1.0", "v2.1.0"))
    }

    @Test
    fun testVersionComparisonWithSuffixes() {
        // Release is newer than snapshot
        assertTrue(AppUpdater.compareVersions("1.15.0", "1.15.0-SNAPSHOT") > 0)
        assertTrue(AppUpdater.compareVersions("1.15.0", "1.15.0-SNAPSHOT-miuix") > 0)
        assertTrue(AppUpdater.compareVersions("v1.15.1", "1.15.0") > 0)
        assertTrue(AppUpdater.compareVersions("1.15.0", "1.15.1") < 0)
        // Same release version
        assertEquals(0, AppUpdater.compareVersions("1.15.0", "1.15.0"))
        assertEquals(0, AppUpdater.compareVersions("1.15.0-miuix", "1.15.0-miuix"))
    }

    @Test
    fun testAssetMatchingFiltersOutNonApk() {
        val assets = listOf(
            tachiyomi.data.release.GitHubAssets(
                url = "https://api.github.com/assets/1",
                name = "EhViewer-1.15.0-default-mapping.txt",
                browserDownloadUrl = "https://github.com/download/mapping.txt",
            ),
            tachiyomi.data.release.GitHubAssets(
                url = "https://api.github.com/assets/2",
                name = "EhViewer-1.15.0-default-arm64-v8a.apk",
                browserDownloadUrl = "https://github.com/download/EhViewer-1.15.0-default-arm64-v8a.apk",
            ),
            tachiyomi.data.release.GitHubAssets(
                url = "https://api.github.com/assets/3",
                name = "EhViewer-1.15.0-default-universal.apk",
                browserDownloadUrl = "https://github.com/download/EhViewer-1.15.0-default-universal.apk",
            ),
        )
        val release = tachiyomi.data.release.GithubRelease(
            version = "1.15.0",
            info = "changelog",
            releaseLink = "https://github.com/releases/1.15.0",
            assets = assets,
        )

        val matched = release.getMatchedAsset()
        assertNotNull(matched)
        assertTrue("Matched asset must be an APK", matched!!.name.endsWith(".apk"))

        val link = release.getDownloadLink()
        assertTrue("Download link must be APK download URL", link.endsWith(".apk"))
        assertTrue("Download link should prefer browserDownloadUrl", link.startsWith("https://github.com/download/"))
    }

    @Test
    fun testReleaseHasUpdateState() {
        val updateRelease = com.hippo.ehviewer.updater.Release(
            version = "1.16.0",
            changelog = "new version changelog",
            downloadLink = "https://github.com/download/EhViewer-1.16.0.apk",
            hasUpdate = true,
        )
        assertTrue(updateRelease.hasUpdate)

        val currentRelease = com.hippo.ehviewer.updater.Release(
            version = "1.15.0",
            changelog = "current version changelog",
            downloadLink = "https://github.com/download/EhViewer-1.15.0.apk",
            hasUpdate = false,
        )
        org.junit.Assert.assertFalse(currentRelease.hasUpdate)
    }
}
