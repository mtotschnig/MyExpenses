package myiconpack

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val PaneSplit: ImageVector
    get() {
        if (_paneSplit != null) {
            return _paneSplit!!
        }
        _paneSplit = Builder(
            name = "PaneSplit",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF000000)),
                pathFillType = NonZero
            ) {
                // Outer shell
                moveTo(19.0f, 4.0f)
                lineTo(5.0f, 4.0f)
                arcTo(2.0f, 2.0f, 0.0f, false, false, 3.0f, 6.0f)
                lineTo(3.0f, 18.0f)
                arcTo(2.0f, 2.0f, 0.0f, false, false, 5.0f, 20.0f)
                lineTo(19.0f, 20.0f)
                arcTo(2.0f, 2.0f, 0.0f, false, false, 21.0f, 18.0f)
                lineTo(21.0f, 6.0f)
                arcTo(2.0f, 2.0f, 0.0f, false, false, 19.0f, 4.0f)
                close()
                // Left pane cutout (x = 5 to 9)
                moveTo(9.0f, 18.0f)
                lineTo(5.0f, 18.0f)
                lineTo(5.0f, 6.0f)
                lineTo(9.0f, 6.0f)
                lineTo(9.0f, 18.0f)
                close()
                // Right pane cutout (x = 11 to 19)
                moveTo(19.0f, 18.0f)
                lineTo(11.0f, 18.0f)
                lineTo(11.0f, 6.0f)
                lineTo(19.0f, 6.0f)
                lineTo(19.0f, 18.0f)
                close()
            }
        }.build()
        return _paneSplit!!
    }

private var _paneSplit: ImageVector? = null