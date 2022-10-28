package org.totschnig.myexpenses.compose

import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.viewmodel.data.ExtraIcon
import org.totschnig.myexpenses.viewmodel.data.IIconInfo
import org.totschnig.myexpenses.viewmodel.data.IconInfo

@Composable
fun Icon(icon: String) {
    val iconInfo = IIconInfo.resolveIcon(icon)
    if (iconInfo == null) {
        Text(color = Color.Red, text = icon)
    } else {
        Icon(iconInfo)
    }
}

@Composable
fun Icon(iconInfo: IIconInfo) {
    when (iconInfo) {
        is ExtraIcon -> {
            androidx.compose.material.Icon(
                modifier = Modifier.size(30.dp),
                painter = painterResource(iconInfo.drawable),
                contentDescription = stringResource(id = iconInfo.label)
            )
        }
        is IconInfo -> {
            Text(
                text = iconInfo.unicode.toString(),
                fontFamily = remember { FontFamily(Font(iconInfo.font, FontWeight.Normal)) },
                fontSize = with(LocalDensity.current) { 24.dp.toSp() }
            )
        }
    }
}

@Preview
@Composable
fun IconTest() {
    Icon(icon = "apple")
}