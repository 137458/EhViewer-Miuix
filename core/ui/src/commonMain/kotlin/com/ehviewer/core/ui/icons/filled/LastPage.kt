package com.ehviewer.core.ui.icons.filled

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import com.ehviewer.core.ui.icons.EhIcons
import com.ehviewer.core.util.unsafeLazy

val EhIcons.Filled.LastPage by unsafeLazy {
    materialIcon(name = "AutoMirrored.Filled.LastPage", autoMirror = true) {
        materialPath {
            moveTo(5.59f, 7.41f)
            lineTo(10.18f, 12.0f)
            lineToRelative(-4.59f, 4.59f)
            lineTo(7.0f, 18.0f)
            lineToRelative(6.0f, -6.0f)
            lineToRelative(-6.0f, -6.0f)
            close()
            moveTo(16.0f, 6.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(12.0f)
            horizontalLineToRelative(-2.0f)
            close()
        }
    }
}
