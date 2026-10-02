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
        DesktopShortcutEntry("Ctrl + Shift + Tab", MR.strings.shortcut_cycle_window_reverse),
        DesktopShortcutEntry("Escape", MR.strings.shortcut_escape),
        DesktopShortcutEntry("↑ / ↓", MR.strings.shortcut_navigate_list),
        DesktopShortcutEntry("Home / End", MR.strings.shortcut_jump_first_last),
        DesktopShortcutEntry("Enter", MR.strings.shortcut_open_selected),
        DesktopShortcutEntry("Double Click", MR.strings.shortcut_open_gallery),
        DesktopShortcutEntry("Drag & Drop", MR.strings.shortcut_drop_link),
        DesktopShortcutEntry("Right Click", MR.strings.shortcut_context_menu),
        DesktopShortcutEntry("F1", MR.strings.shortcut_show_help),
    )

    // 阅读窗口键位分组（F1 指南第二段）；←/→ 与 PageUp/PageDown/Space 翻页随阅读方向反转，Home/End 恒跳首/末页
    fun readerEntries(): List<DesktopShortcutEntry> = listOf(
        DesktopShortcutEntry("← / → / PageUp / PageDown / Space", MR.strings.shortcut_reader_paging),
        DesktopShortcutEntry("Home / End", MR.strings.shortcut_reader_jump_first_last),
        DesktopShortcutEntry("Ctrl + = / Ctrl + - / Ctrl + 0", MR.strings.desktop_reader_zoom),
        DesktopShortcutEntry("Escape", MR.strings.shortcut_escape),
        DesktopShortcutEntry("F1", MR.strings.shortcut_show_help),
    )
}
