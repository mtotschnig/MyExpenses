package org.totschnig.myexpenses.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.TextAutoSizeDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import org.totschnig.myexpenses.viewmodel.data.ExtraIcon
import org.totschnig.myexpenses.viewmodel.data.IIconInfo
import org.totschnig.myexpenses.viewmodel.data.IconInfo

@Composable
fun Icon(
    icon: String,
    modifier: Modifier = Modifier,
    size: TextUnit? = 24.sp,
    color: Color? = null,
) {
    val iconInfo = IIconInfo.resolveIcon(icon)
    if (iconInfo == null) {
        Text(color = Color.Red, text = icon, modifier = modifier)
    } else {
        Icon(iconInfo, modifier, size, color)
    }
}

@Composable
fun Icon(
    iconInfo: IIconInfo,
    modifier: Modifier = Modifier,
    size: TextUnit? = 24.sp,
    color: Color? = null,
) {
    when (iconInfo) {
        is ExtraIcon -> {
            Icon(
                modifier = modifier.optional(size) {
                    size(it)
                },
                painter = rememberDrawablePainter(drawable = iconInfo.asDrawable(LocalContext.current)),
                contentDescription = stringResource(id = iconInfo.label),
                tint = color ?: LocalContentColor.current
            )
        }

        is IconInfo -> {
            CharIcon(
                char = iconInfo.unicode,
                modifier = modifier,
                fontFamily = iconInfo.fontFamily,
                size = size,
                color = color
            )
        }
    }
}

/**
 * if @param size is null, use AutoSize
 */
@Composable
fun CharIcon(
    char: Char,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily? = null,
    size: TextUnit? = 24.sp,
    color: Color? = null,
) {
    if (size == null) {
        BasicText(
            modifier = modifier,
            text = char.toString(),
            style = TextStyle(
                fontFamily = fontFamily,
                color = color ?: LocalContentColor.current
            ),
            autoSize = TextAutoSize.StepBased(minFontSize = 6.sp)
        )
    } else {
        Text(
            modifier = modifier,
            text = char.toString(),
            style = TextStyle(
                lineHeight = size
            ),
            fontFamily = fontFamily,
            fontSize = size,
            color = color ?: Color.Unspecified
        )
    }
}


@Preview
@Composable
private fun IconTest() {
    Icon(icon = "apple")
}