package org.totschnig.myexpenses.compose

import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.totschnig.myexpenses.R
import timber.log.Timber

data class IconInfo(val unicode: String, val label: String, val isBrand: Boolean)

fun LazyGridScope.header(content: String) {
    item(span = { GridItemSpan(this.maxLineSpan) }) {
        Text(text = content, style = MaterialTheme.typography.h6)
    }
}

@Composable
fun IconSelector(
    categories: Array<String>,
    labelForCategory: (String) -> String,
    iconsForCategory: (String) -> Array<IconInfo>
) {
    val faFontFamilyBrand = FontFamily(Font(R.font.fa_brands_400, FontWeight.Normal))
    val faFontFamilySolid = FontFamily(Font(R.font.fa_solid_900, FontWeight.Normal))
    val ctx = LocalContext.current
    LazyVerticalGrid(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(R.dimen.activity_horizontal_margin))
            .padding(top = 12.dp, bottom = 4.dp),
        columns = GridCells.Adaptive(40.dp)
    ) {
        for (category in categories) {
            Timber.d(category)
            header(labelForCategory(category))
            val icons = iconsForCategory(category)
/*            for (icon in icons) {
                item {
                    Text(
                        modifier = Modifier.padding(horizontal = 8.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = { Toast.makeText(ctx, icon.label, Toast.LENGTH_LONG).show() }
                                )
                            },
                        text = icon.unicode,
                        fontFamily = if(icon.isBrand) faFontFamilyBrand else faFontFamilySolid,
                        fontSize = 24.sp,
                        color = LocalColors.current.iconTint
                    )
                }
            }*/
        }
    }
}

@Preview
@Composable
fun Preview() {
    val ctx = LocalContext.current
    IconSelector(
        stringArrayResource(id = R.array.categories),
        labelForCategory = {
            "Accessibility"
        },
        iconsForCategory = {
            Array(5) {
                IconInfo(ctx.getString(R.string.fa_audio_description_unicode), it.toString(), false)
            }
        }
    )
}