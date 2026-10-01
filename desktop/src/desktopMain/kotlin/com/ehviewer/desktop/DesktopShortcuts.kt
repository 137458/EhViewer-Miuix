package com.ehviewer.desktop

import com.ehviewer.core.i18n.MR
import dev.icerock.moko.resources.StringResource

data class DesktopShortcutEntry(
    val keyCombination: String,
    val descriptionRes: StringResource,
)

object DesktopShortcuts {
    fun defaultEntries(): List<DesktopShortcutEntry> = listOf(
        DesktopShortcutEntry("Ctrl + W / Ctrl + Q", MR.strings.shortcut_close_window),
        DesktopShortcutEntry("Ctrl + R / F5", MR.strings.shortcut_refresh_galleries),
        DesktopShortcutEntry("Ctrl + O", MR.strings.shortcut_open_gallery_by_link),
        DesktopShortcutEntry("Ctrl + Tab", MR.strings.shortcut_cycle_window),
        DesktopShortcutEntry("Escape", MR.strings.shortcut_escape),
        DesktopShortcutEntry("↑ / ↓", MR.strings.shortcut_navigate_list),
        DesktopShortcutEntry("Home / End", MR.strings.shortcut_jump_first_last),
        DesktopShortcutEntry("Enter", MR.strings.shortcut_open_selected),
        DesktopShortcutEntry("Double Click", MR.strings.shortcut_open_gallery),
        DesktopShortcutEntry("Right Click", MR.strings.shortcut_context_menu),
        DesktopShortcutEntry("F1", MR.strings.shortcut_show_help),
    )
}
