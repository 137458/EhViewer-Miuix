package com.ehviewer.desktop

import com.ehviewer.core.i18n.MR
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.StringDesc
import kotlin.test.Test
import kotlin.test.assertTrue

class DesktopI18nTest {

    @Test
    fun desktopSpecificStringsResolveProperly() {
        val minimizeToTray = StringDesc.Resource(MR.strings.settings_close_minimize_to_tray).localized()
        assertTrue(minimizeToTray.isNotEmpty())

        val openApp = StringDesc.Resource(MR.strings.tray_open_app).localized()
        assertTrue(openApp.isNotEmpty())

        val searchHint = StringDesc.Resource(MR.strings.search_hint).localized()
        assertTrue(searchHint.isNotEmpty())

        val openBrowser = StringDesc.Resource(MR.strings.open_in_browser).localized()
        assertTrue(openBrowser.isNotEmpty())

        val copyLink = StringDesc.Resource(MR.strings.copy_link).localized()
        assertTrue(copyLink.isNotEmpty())

        val copyTitle = StringDesc.Resource(MR.strings.copy_title).localized()
        assertTrue(copyTitle.isNotEmpty())

        val online = StringDesc.Resource(MR.strings.online).localized()
        assertTrue(online.isNotEmpty())
    }
}
