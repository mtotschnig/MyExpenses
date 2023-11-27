package org.totschnig.myexpenses.util.ui

import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.ui.AmountEditText
import org.totschnig.myexpenses.ui.AmountInput
import java.math.BigDecimal


fun AmountInput.requireAmountInput(
    currencyUnit: CurrencyUnit
): Result<Money> = validateAmountInput(currencyUnit, ifPresent = false).mapCatching { it!! }


fun AmountInput.validateAmountInput(
    showToUser: Boolean,
    ifPresent: Boolean
): BigDecimal? = getTypedValue(ifPresent, showToUser)

fun AmountInput.validateAmountInput(
    currencyUnit: CurrencyUnit,
    showToUser: Boolean = true,
    ifPresent: Boolean = true
): Result<Money?> {
    val result = validateAmountInput(ifPresent = ifPresent, showToUser = showToUser)
    return if (result == null) Result.success(null) else
        runCatching {
            try {
                Money(currencyUnit, result)
            } catch (e: ArithmeticException) {
                if (showToUser) {
                    setError("Number too large.")
                }
                throw e
            }
        }
}

fun AmountEditText.validateAmountInput(currencyUnit: CurrencyUnit) = validate(true)?.let {
    try { Money(currencyUnit, it) }
    catch (e: ArithmeticException) {
        error = "Number too large."
        null
    }
}
