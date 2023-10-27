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
    SPARDA(R.drawable.sparda, Color.rgb(0, 92, 168))
}

val Bank.asWellKnown: WellKnownBank?
    get() = when {
        blz == "12030000" -> WellKnownBank.DKB
        blz == "43060967" -> WellKnownBank.GLS
        blz == "50010517" -> WellKnownBank.ING
        blz.startsWith("200411") -> WellKnownBank.COMDIRECT
        blz[3] == '5' -> WellKnownBank.SPARKASSE
        blz[3] == '9' -> WellKnownBank.VOLKSBANK
        bankName.contains("sparda", ignoreCase = true) -> WellKnownBank.SPARDA
        else -> null
    }