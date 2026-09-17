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
    fun testGenerateMockRelease() {
        val mock = AppUpdater.generateMockRelease()
        assertNotNull(mock)
        assertTrue(mock.version.startsWith("v"))
        assertTrue(mock.downloadLink.isNotBlank())
        assertTrue(mock.changelog.isNotBlank())
        assertTrue(mock.changelog.contains("###"))
        assertTrue(mock.apkSize > 0L)
        assertTrue(mock.releaseTitle.isNotBlank())
    }
}
