package org.totschnig.myexpenses.viewmodel.data

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.res.ResourcesCompat
import com.kazy.fontdrawable.FontDrawable
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler

sealed interface IIconInfo {
    val label: Int
    fun asDrawable(context: Context, @AttrRes colorAttr: Int = com.google.android.material.R.attr.colorOnSurface): Drawable?

    companion object {
        fun resolveIcon(icon: String): IIconInfo? =
            FontAwesomeIcons[icon] ?: ExtraIcons[icon] ?: kotlin.run {
                CrashHandler.report(Exception("Unable to resolve icon $icon"))
                null
            }

        fun resolveLabelForCategory(context: Context, category: String) =
            context.getString(
                context.resources.getIdentifier(
                    "category_${category}_label",
                    "string",
                    context.packageName
                )
            )

        fun resolveIconsForCategory(context: Context, category: String) =
            buildMap {
                context.resources.getStringArray(
                    context.resources.getIdentifier(
                        "category_${category}_icons",
                        "array",
                        context.packageName
                    )
                ).forEach {
                    put(
                        it,
                        FontAwesomeIcons[it]
                            ?: throw IllegalArgumentException("no icon $it")
                    )
                }
                context.resources.getIdentifier(
                    "extra_${category}_icons", "array", context.packageName
                ).takeIf { it != 0 }?.let { resId ->
                    context.resources.getStringArray(resId).forEach {
                        put(
                            it,
                            ExtraIcons[it]
                                ?: throw IllegalArgumentException("no icon $it")
                        )
                    }
                }
            }

        fun searchIcons(context: Context, searchTerm: String) =
            (FontAwesomeIcons + ExtraIcons).filter {
                context.getString(it.value.label).contains(
                    searchTerm,
                    true
                )
            }
    }
}

val fontAwesomeSolid = FontFamily(Font(R.font.fa_solid_900, FontWeight.Normal))
val fontAwesomeBrand = FontFamily(Font(R.font.fa_brands_400, FontWeight.Normal))

data class IconInfo(val unicode: Char, @StringRes override val label: Int, val isBrand: Boolean) :
    IIconInfo {
    val font = if (isBrand) R.font.fa_brands_400 else R.font.fa_solid_900
    val fontFamily = if (isBrand) fontAwesomeBrand else fontAwesomeSolid
    override fun asDrawable(context: Context, @AttrRes colorAttr: Int): Drawable? =
        FontDrawable.Builder(context, unicode, ResourcesCompat.getFont(context, font))
            .setSizeDp(24)
            .setColor(UiUtils.getColor(context, colorAttr))
            .build()
}

data class ExtraIcon(@DrawableRes val drawable: Int, @StringRes override val label: Int) :
    IIconInfo {
    override fun asDrawable(context: Context, @AttrRes colorAttr: Int): Drawable? =
        AppCompatResources.getDrawable(context, drawable)?.apply {
            setTint(UiUtils.getColor(context, colorAttr))
        }
}