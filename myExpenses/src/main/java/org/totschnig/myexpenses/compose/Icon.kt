package org.totschnig.myexpenses.compose

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import org.totschnig.myexpenses.viewmodel.data.ExtraIcon
import org.totschnig.myexpenses.viewmodel.data.IIconInfo
import org.totschnig.myexpenses.viewmodel.data.IconInfo

@Composable
fun Icon(icon: String, size: TextUnit = 24.sp, color: Color? = null) {
    val iconInfo = IIconInfo.resolveIcon(icon)
    if (iconInfo == null) {
        Text(color = Color.Red, text = icon)
    } else {
        Icon(iconInfo, size, color)
    }
}

@Composable
fun Icon(iconInfo: IIconInfo, size: TextUnit = 24.sp, color: Color? = null) {
    when (iconInfo) {
        is ExtraIcon -> {
            Icon(
                modifier = Modifier.size(size * 1.25f),
                painter = rememberDrawablePainter(drawable = iconInfo.asDrawable(LocalContext.current)),
                contentDescription = stringResource(id = iconInfo.label),
                tint = color ?: LocalContentColor.current
            )
        }
        is IconInfo -> {
            CharIcon(iconInfo.unicode, iconInfo.fontFamily, size, color)
        }
    }
}
@Composable
fun CharIcon(
    char: Char,
    fontFamily: FontFamily? = null,
    size: TextUnit = 24.sp,
    color: Color? = null
) {
    Text(
        text = char.toString(),
        fontFamily = fontFamily,
        fontSize = size,
        color = color ?: Color.Unspecified
    )
}



@Preview
@Composable
fun IconTest() {
    Icon(icon = "apple")
}