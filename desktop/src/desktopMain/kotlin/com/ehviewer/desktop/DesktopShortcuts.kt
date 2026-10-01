package com.ehviewer.desktop

data class DesktopShortcutEntry(
    val keyCombination: String,
    val description: String,
)

object DesktopShortcuts {
    fun defaultEntries(): List<DesktopShortcutEntry> = listOf(
        DesktopShortcutEntry("Ctrl + W / Ctrl + Q", "Close window"),
        DesktopShortcutEntry("Ctrl + R / F5", "Refresh galleries"),
        DesktopShortcutEntry("Escape", "Clear selection / Cancel search / Close window"),
        DesktopShortcutEntry("Double Click", "Open gallery in standalone window"),
        DesktopShortcutEntry("Right Click", "Context menu (copy, open browser, delete)"),
        DesktopShortcutEntry("F1", "Show keyboard shortcuts"),
    )
}
