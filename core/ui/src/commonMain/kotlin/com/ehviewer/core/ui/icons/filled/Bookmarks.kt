package com.ehviewer.core.ui.icons.filled

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import com.ehviewer.core.ui.icons.EhIcons
import com.ehviewer.core.util.unsafeLazy

val EhIcons.Filled.Bookmarks by unsafeLazy {
    materialIcon(name = "Filled.Bookmarks") {
        materialPath {
            moveTo(19.0f, 18.0f)
            lineToRelative(2.0f, 1.0f)
            verticalLineTo(3.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            horizontalLineTo(8.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(1.0f)
            horizontalLineToRelative(11.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, 0.9f, 2.0f, 2.0f)
            verticalLineToRelative(14.0f)
            close()
            moveTo(15.0f, 5.0f)
            horizontalLineTo(5.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(16.0f)
            lineToRelative(7.0f, -3.0f)
            lineToRelative(7.0f, 3.0f)
            verticalLineTo(7.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            close()
        }
    }
}
