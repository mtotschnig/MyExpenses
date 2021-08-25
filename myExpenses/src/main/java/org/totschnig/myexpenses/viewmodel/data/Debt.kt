package org.totschnig.myexpenses.viewmodel.data

import android.content.ContentValues
import org.threeten.bp.LocalDate
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DESCRIPTION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.util.localDate2Epoch
import java.math.BigDecimal

data class Debt(
    val id: Long,
    val label: String,
    val description: String,
    val payeeId: Long,
    val amount: Long,
    val currency: String,
    val date: Long
) {
    constructor(
        id: Long,
        label: String,
        description: String,
        payeeId: Long,
        amount: BigDecimal,
        currency: CurrencyUnit,
        date: LocalDate
    ) : this(
        id,
        label,
        description,
        payeeId,
        Money(currency, amount).amountMinor,
        currency.code,
        localDate2Epoch(date)
    )

    fun toContentValues() = ContentValues().apply {
        put(KEY_LABEL, label)
        put(KEY_DESCRIPTION, description)
        put(KEY_AMOUNT, amount)
        put(KEY_CURRENCY, currency)
        put(KEY_DATE, date)
        if (id == 0L) {
            //the link between debt and payeeId should not be altered
            put(KEY_PAYEEID, payeeId)
        }
    }
}