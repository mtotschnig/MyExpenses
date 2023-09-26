package org.totschnig.myexpenses.model

import android.content.Context
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.enumValueOrNull

enum class PreDefinedPaymentMethod(
    val paymentType: Int,
    val isNumbered: Boolean,
    val resId: Int,
    val icon: String
) {
    CHEQUE(-1, true, R.string.pm_cheque, "money-check"),
    CREDITCARD(-1, false, R.string.pm_creditcard, "credit-card"),
    DEPOSIT(1, false, R.string.pm_deposit, "down-long"),
    DIRECTDEBIT(-1, false, R.string.pm_directdebit, "up-long");

    fun getLocalizedLabel(context: Context): String = context.getString(resId)

    companion object {
        fun String.translateIfPredefined(context: Context) =
            enumValueOrNull<PreDefinedPaymentMethod>(this)?.getLocalizedLabel(context) ?: this
    }
}