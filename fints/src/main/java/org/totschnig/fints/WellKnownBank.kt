package org.totschnig.fints

import android.graphics.Color
import androidx.annotation.DrawableRes
import org.totschnig.myexpenses.model2.Bank

enum class WellKnownBank(@DrawableRes val icon: Int, val color: Int) {
    DKB(R.drawable.dkb, Color.rgb(20, 141, 234)),
    GLS(R.drawable.gls, Color.rgb(38, 230, 101)),
    ING(R.drawable.ing, Color.rgb(255,98,0 )),
    COMDIRECT(R.drawable.comdirect, Color.rgb(255, 245, 0)),
    SPARKASSE(R.drawable.sparkasse, Color.rgb(255, 0, 0)),
    VOLKSBANK(R.drawable.volksbank, Color.rgb(0, 101, 178)),
    SPARDA(R.drawable.sparda, Color.rgb(0, 92, 168)),
    TARGO(R.drawable.targo, Color.rgb(122, 156, 192)),
    DEUTSCHE_BANK(R.drawable.deutsche_bank, Color.rgb(0, 24, 168))
}

val Bank.asWellKnown: WellKnownBank?
    get() = when {
        blz == "30020900" -> WellKnownBank.TARGO
        blz == "12030000" -> WellKnownBank.DKB
        blz == "43060967" -> WellKnownBank.GLS
        blz == "50010517" -> WellKnownBank.ING
        blz.startsWith("200411") -> WellKnownBank.COMDIRECT
        bankName.contains("sparda", ignoreCase = true) -> WellKnownBank.SPARDA
        blz[3] == '5' -> WellKnownBank.SPARKASSE
        blz[3] == '7' -> WellKnownBank.DEUTSCHE_BANK
        blz[3] == '9' || blz[3] == '6' -> WellKnownBank.VOLKSBANK
        else -> null
    }