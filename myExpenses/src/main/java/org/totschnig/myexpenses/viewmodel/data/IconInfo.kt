package org.totschnig.myexpenses.viewmodel.data

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

sealed interface IIconInfo: java.io.Serializable {
    val label: Int
    companion object {
        fun resolveIcon(icon: String): IIconInfo =
            FontAwesomeIcons[icon] ?: ExtraIcons[icon]
            ?: throw IllegalArgumentException("no icon $icon")
    }
}

data class IconInfo(val unicode: Char, @StringRes override val label: Int, val isBrand: Boolean): IIconInfo

data class ExtraIcon(@DrawableRes val drawable: Int, @StringRes override val label: Int): IIconInfo