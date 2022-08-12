package org.totschnig.myexpenses.compose

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import org.totschnig.myexpenses.R
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
            val context = LocalContext.current

            val drawable = AppCompatResources.getDrawable(context, iconInfo.drawable)
            androidx.compose.material.Icon(
                modifier = Modifier
                    .size(30.dp),
                painter = rememberDrawablePainter(drawable = drawable),
                contentDescription = stringResource(id = iconInfo.label)
            )
        }
        is IconInfo -> {
            val faFontFamilyBrand =
                remember { FontFamily(Font(R.font.fa_brands_400, FontWeight.Normal)) }
            val faFontFamilySolid =
                remember { FontFamily(Font(R.font.fa_solid_900, FontWeight.Normal)) }
            Text(
                text = iconInfo.unicode.toString(),
                fontFamily = if (iconInfo.isBrand) faFontFamilyBrand else faFontFamilySolid,
                fontSize = 24.sp,
                color = LocalColors.current.iconTint
            )
        }
    }
}