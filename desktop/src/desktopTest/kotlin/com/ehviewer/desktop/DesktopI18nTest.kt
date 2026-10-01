package com.ehviewer.desktop

import com.ehviewer.core.i18n.MR
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlin.test.Test
import kotlin.test.assertTrue

class DesktopI18nTest {

    @Test
    fun desktopSpecificStringsResolveProperly() {
        val closeBehavior = StringDesc.Resource(MR.strings.settings_close_behavior).localized()
        assertTrue(closeBehavior.isNotEmpty())

        val minimizeToTray = StringDesc.Resource(MR.strings.settings_close_minimize_to_tray).localized()
        assertTrue(minimizeToTray.isNotEmpty())

        val exitApp = StringDesc.Resource(MR.strings.settings_close_exit).localized()
        assertTrue(exitApp.isNotEmpty())

        val openApp = StringDesc.Resource(MR.strings.tray_open_app).localized()
        assertTrue(openApp.isNotEmpty())

        val searchHint = StringDesc.Resource(MR.strings.search_hint).localized()
        assertTrue(searchHint.isNotEmpty())
    }
}
