package org.totschnig.fints

import android.graphics.Color
import androidx.annotation.DrawableRes
import org.totschnig.myexpenses.model2.Bank

enum class WellKnownBank(@DrawableRes val icon: Int, val color: Int, vararg val blz: String) {
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
    RENAULT(R.drawable.renault, Color.rgb(128, 128, 128), "30520037"),
    MLP(R.drawable.mlp, Color.rgb(190, 182, 170), "67230000"),
    DIREKT1822(R.drawable.direkt1822_96, Color.rgb(0, 58, 107), "50050222"),
    PSD(R.drawable.psd, Color.rgb(1, 153, 102)),
    COMMERZBANK(R.drawable.commerz, Color.rgb(254, 213, 41)),
    KD(R.drawable.kd, Color.rgb(162, 181, 45), "10061006", "44064406", "81068106", "35060190")
}

val Bank.asWellKnown: WellKnownBank?
    get() = WellKnownBank.entries.find { it.blz.contains(blz) } ?: when {
        blz.startsWith("200411") -> WellKnownBank.COMDIRECT
        bankName.contains("Commerzbank") -> WellKnownBank.COMMERZBANK
        bankName.contains("sparda", ignoreCase = true) -> WellKnownBank.SPARDA
        bankName.contains("postbank", ignoreCase = true) -> WellKnownBank.POSTBANK
        bankName.contains("PSD Bank", ignoreCase = true) -> WellKnownBank.PSD
        blz[3] == '5' -> WellKnownBank.SPARKASSE
        blz[3] == '7' -> WellKnownBank.DEUTSCHE_BANK
        blz[3] == '9' || blz[3] == '6' -> WellKnownBank.VOLKSBANK
        else -> null
    }