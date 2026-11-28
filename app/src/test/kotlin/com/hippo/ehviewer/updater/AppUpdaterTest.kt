package com.hippo.ehviewer.updater

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppUpdaterTest {
    @Test
    fun `compareVersions treats missing patch as zero`() {
        assertEquals(0, AppUpdater.compareVersions("1.16.0", "1.16"))
        assertEquals(0, AppUpdater.compareVersions("v1.16", "1.16.0"))
        assertEquals(0, AppUpdater.compareVersions("1.16", "1.16.0.0"))
    }

    @Test
    fun `compareVersions compares prerelease counters numerically`() {
        assertTrue(AppUpdater.compareVersions("1.16.0-beta2", "1.16.0-beta10") < 0)
        assertTrue(AppUpdater.compareVersions("1.16.0-beta10", "1.16.0-beta2") > 0)
        assertEquals(0, AppUpdater.compareVersions("1.16.0-beta2", "1.16.0-beta2"))
    }

    @Test
    fun `compareVersions ranks prerelease below release`() {
        assertTrue(AppUpdater.compareVersions("1.16.0", "1.16.0-beta10") > 0)
        assertTrue(AppUpdater.compareVersions("1.16.0-rc1", "1.16.0") < 0)
    }

    @Test
    fun `compareVersions still ranks releases by number`() {
        assertTrue(AppUpdater.compareVersions("1.17.0", "1.16.9") > 0)
        assertTrue(AppUpdater.compareVersions("1.16.0", "1.17.0") < 0)
    }

    @Test
    fun `only https github hosts are accepted as update source`() {
        assertTrue(AppUpdater.isAllowedUpdateUrl("https://github.com/o/r/releases/download/v1/app.apk"))
        assertTrue(AppUpdater.isAllowedUpdateUrl("https://api.github.com/repos/o/r/actions/artifacts/1/zip"))
        assertTrue(AppUpdater.isAllowedUpdateUrl("https://objects.githubusercontent.com/o/r/app.apk"))
        assertFalse(AppUpdater.isAllowedUpdateUrl("http://github.com/o/r/app.apk"))
        assertFalse(AppUpdater.isAllowedUpdateUrl("https://evil.example.com/app.apk"))
        assertFalse(AppUpdater.isAllowedUpdateUrl("https://github.com.evil.example.com/app.apk"))
        assertFalse(AppUpdater.isAllowedUpdateUrl("not a url"))
    }

    @Test
    fun `archive detection follows content type before url`() {
        assertTrue(AppUpdater.isZipResponse("application/zip; charset=binary", "https://api.github.com/repos/o/r/actions/artifacts/1/zip"))
        assertTrue(AppUpdater.isZipResponse("application/zip", "https://github.com/o/r/app.apk"))
        assertFalse(AppUpdater.isZipResponse("application/vnd.android.package-archive", "https://github.com/o/r/app.zip"))
        assertTrue(AppUpdater.isZipResponse(null, "https://github.com/o/r/app.zip?x=1"))
        assertFalse(AppUpdater.isZipResponse(null, "https://github.com/o/r/app.apk"))
    }

    @Test
    fun `apk entry selection skips directories and other files`() {
        assertTrue(AppUpdater.isApkEntry("app-default-release.apk", false))
        assertTrue(AppUpdater.isApkEntry("APP.APK", false))
        assertFalse(AppUpdater.isApkEntry("mapping.txt", false))
        assertFalse(AppUpdater.isApkEntry("app.apk", true))
    }
}
