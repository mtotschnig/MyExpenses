package org.totschnig.myexpenses.next

import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.next.core.model.ledger.CurrencyInfo
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.convAmount
import org.totschnig.myexpenses.next.core.ui.AmountFormatter

/** The currency as the features of the new UI know it */
fun CurrencyUnit.toInfo() = CurrencyInfo(code, symbol, fractionDigits)

/** The currency of the main module for the parts of the new UI that still work with it */
fun CurrencyInfo.toUnit() = CurrencyUnit(code, symbol, fractionDigits)

/** Formats amounts of the features like the rest of the app */
fun ICurrencyFormatter.toAmountFormatter() = AmountFormatter { amount, currency ->
    convAmount(amount, currency.toUnit())
}

