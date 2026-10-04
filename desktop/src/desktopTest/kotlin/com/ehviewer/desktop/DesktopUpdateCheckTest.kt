package com.ehviewer.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class DesktopUpdateCheckTest {
    @Test
    fun newerReleaseMapsToAvailable() = runBlocking {
        val info = UpdateInfo("v1.16.0", RELEASES_PAGE_URL)
        assertEquals(UpdateCheckResult.Available(info), checkLatestReleaseStatus(currentVersion = "1.15.0") { info })
    }

    @Test
    fun sameOrOlderReleaseMapsToUpToDate() = runBlocking {
        assertEquals(
            UpdateCheckResult.UpToDate,
            checkLatestReleaseStatus("1.15.0") { UpdateInfo("v1.15.0", RELEASES_PAGE_URL) },
        )
        assertEquals(
            UpdateCheckResult.UpToDate,
            checkLatestReleaseStatus("1.15.0") { UpdateInfo("v1.14.9", RELEASES_PAGE_URL) },
        )
    }

    @Test
    fun fetchFailureMapsToUnavailable() = runBlocking {
        assertEquals(UpdateCheckResult.Unavailable, checkLatestReleaseStatus("1.15.0") { null })
    }
}
