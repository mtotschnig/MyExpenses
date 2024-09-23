package org.totschnig.myexpenses.viewmodel.data

import android.content.ContentValues
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DESCRIPTION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.util.toEpoch
import java.time.LocalDate

data class Debt(
    val id: Long,
    val label: String,
    val description: String,
    val payeeId: Long,
    val amount: Long,
    val currency: CurrencyUnit,
    val date: Long,
    val equivalentAmount: Long? = null,
) {
    constructor(
        id: Long,
        label: String,
        description: String,
        payeeId: Long,
        amount: Long,
        currency: CurrencyUnit,
        date: LocalDate,
        equivalentAmount: Long?
    ) : this(
        id,
        label,
        description,
        payeeId,
        amount,
        currency,
        date.toEpoch(),
        equivalentAmount = equivalentAmount
    )

    fun toContentValues() = ContentValues().apply {
        put(KEY_LABEL, label)
        put(KEY_DESCRIPTION, description)
        put(KEY_AMOUNT, amount)
        put(KEY_CURRENCY, currency.code)
        put(KEY_DATE, date)
        if (id == 0L) {
            //the link between debt and payeeId should not be altered
            put(KEY_PAYEEID, payeeId)
        }
        equivalentAmount?.let {
            put(KEY_EQUIVALENT_AMOUNT, it)
        }
    }
}