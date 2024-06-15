package org.totschnig.fints

import android.graphics.Color
import androidx.annotation.DrawableRes
import org.totschnig.myexpenses.model2.Bank

enum class WellKnownBank(@DrawableRes val icon: Int, val color: Int, val blz: String? = null) {
    DKB(R.drawable.dkb, Color.rgb(20, 141, 234), "12030000"),
    GLS(R.drawable.gls, Color.rgb(38, 230, 101), "43060967"),
    ING(R.drawable.ing, Color.rgb(255, 98, 0), "50010517"),
    COMDIRECT(R.drawable.comdirect, Color.rgb(255, 245, 0)),
    SPARKASSE(R.drawable.sparkasse, Color.rgb(255, 0, 0)),
    VOLKSBANK(R.drawable.volksbank, Color.rgb(0, 101, 178)),
    SPARDA(R.drawable.sparda, Color.rgb(0, 92, 168)),
    TARGO(R.drawable.targo, Color.rgb(122, 156, 192), "30020900"),
    DEUTSCHE_BANK(R.drawable.deutsche_bank, Color.rgb(0, 24, 168)),
    POSTBANK(R.drawable.postbank, Color.rgb(255, 204, 0)),
    LIGA(R.drawable.liga, Color.rgb(2, 41, 164), "75090300"),
    RENAULT(R.drawable.renault, Color.rgb(128, 128, 128), "30520037")
}

val Bank.asWellKnown: WellKnownBank?
    get() = WellKnownBank.entries.find { it.blz == blz } ?: when {
        blz.startsWith("200411") -> WellKnownBank.COMDIRECT
        bankName.contains("sparda", ignoreCase = true) -> WellKnownBank.SPARDA
        bankName.contains("postbank", ignoreCase = true) -> WellKnownBank.POSTBANK
        blz[3] == '5' -> WellKnownBank.SPARKASSE
        blz[3] == '7' -> WellKnownBank.DEUTSCHE_BANK
        blz[3] == '9' || blz[3] == '6' -> WellKnownBank.VOLKSBANK
        else -> null
    }