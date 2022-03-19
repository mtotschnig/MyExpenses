package myiconpack

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

//https://fontawesome.com/icons/up-down-left-right CC BY 4.0 License
val ArrowsAlt: ImageVector
    get() {
        if (_arrowsAlt != null) {
            return _arrowsAlt!!
        }
        _arrowsAlt = Builder(name = "Arrows-alt", defaultWidth = 24.dp, defaultHeight =
                24.dp, viewportWidth = 512.0f, viewportHeight = 512.0f).apply {
            path(fill = SolidColor(Color(0xFF000000)), stroke = null, strokeLineWidth = 0.0f,
                    strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                    pathFillType = NonZero) {
                moveTo(352.201f, 425.775f)
                lineToRelative(-79.196f, 79.196f)
                curveToRelative(-9.373f, 9.373f, -24.568f, 9.373f, -33.941f, 0.0f)
                lineToRelative(-79.196f, -79.196f)
                curveToRelative(-15.119f, -15.119f, -4.411f, -40.971f, 16.971f, -40.97f)
                horizontalLineToRelative(51.162f)
                lineTo(228.0f, 284.0f)
                horizontalLineTo(127.196f)
                verticalLineToRelative(51.162f)
                curveToRelative(0.0f, 21.382f, -25.851f, 32.09f, -40.971f, 16.971f)
                lineTo(7.029f, 272.937f)
                curveToRelative(-9.373f, -9.373f, -9.373f, -24.569f, 0.0f, -33.941f)
                lineTo(86.225f, 159.8f)
                curveToRelative(15.119f, -15.119f, 40.971f, -4.411f, 40.971f, 16.971f)
                verticalLineTo(228.0f)
                horizontalLineTo(228.0f)
                verticalLineTo(127.196f)
                horizontalLineToRelative(-51.23f)
                curveToRelative(-21.382f, 0.0f, -32.09f, -25.851f, -16.971f, -40.971f)
                lineToRelative(79.196f, -79.196f)
                curveToRelative(9.373f, -9.373f, 24.568f, -9.373f, 33.941f, 0.0f)
                lineToRelative(79.196f, 79.196f)
                curveToRelative(15.119f, 15.119f, 4.411f, 40.971f, -16.971f, 40.971f)
                horizontalLineToRelative(-51.162f)
                verticalLineTo(228.0f)
                horizontalLineToRelative(100.804f)
                verticalLineToRelative(-51.162f)
                curveToRelative(0.0f, -21.382f, 25.851f, -32.09f, 40.97f, -16.971f)
                lineToRelative(79.196f, 79.196f)
                curveToRelative(9.373f, 9.373f, 9.373f, 24.569f, 0.0f, 33.941f)
                lineTo(425.773f, 352.2f)
                curveToRelative(-15.119f, 15.119f, -40.971f, 4.411f, -40.97f, -16.971f)
                verticalLineTo(284.0f)
                horizontalLineTo(284.0f)
                verticalLineToRelative(100.804f)
                horizontalLineToRelative(51.23f)
                curveToRelative(21.382f, 0.0f, 32.09f, 25.851f, 16.971f, 40.971f)
                close()
            }
        }
        .build()
        return _arrowsAlt!!
    }

private var _arrowsAlt: ImageVector? = null
