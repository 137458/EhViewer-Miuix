package com.ehviewer.core.ui.icons.filled

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import com.ehviewer.core.ui.icons.EhIcons
import com.ehviewer.core.util.unsafeLazy

val EhIcons.Filled.Shuffle by unsafeLazy {
    materialIcon(name = "Filled.Shuffle") {
        materialPath {
            moveTo(10.59f, 9.17f)
            lineTo(5.41f, 4.0f)
            lineTo(4.0f, 5.41f)
            lineToRelative(5.17f, 5.17f)
            lineToRelative(1.42f, -1.41f)
            close()
            moveTo(14.5f, 4.0f)
            lineToRelative(2.04f, 2.04f)
            lineTo(4.0f, 18.59f)
            lineTo(5.41f, 20.0f)
            lineTo(17.96f, 7.46f)
            lineTo(20.0f, 9.5f)
            lineTo(20.0f, 4.0f)
            horizontalLineToRelative(-5.5f)
            close()
            moveTo(14.83f, 13.41f)
            lineToRelative(-1.41f, 1.41f)
            lineToRelative(3.13f, 3.13f)
            lineTo(14.5f, 20.0f)
            lineTo(20.0f, 20.0f)
            verticalLineToRelative(-5.5f)
            lineToRelative(-2.04f, 2.04f)
            lineToRelative(-3.13f, -3.13f)
            close()
        }
    }
}
