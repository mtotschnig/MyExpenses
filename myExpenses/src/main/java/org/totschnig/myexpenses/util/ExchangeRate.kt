package org.totschnig.myexpenses.util

import org.totschnig.myexpenses.model.CurrencyUnit
import java.math.BigDecimal

/**
 * real exchange rate relates major units of two currencies.
 */
fun calculateRealExchangeRate(
    raw: Double,
    currencyUnit: CurrencyUnit,
    homeCurrency: CurrencyUnit
): BigDecimal = BigDecimal.valueOf(raw).movePointRight(currencyUnit.minorUnitDelta(homeCurrency))

/**
 * raw exchange rate relates minor units of two currencies.
 */
fun calculateRawExchangeRate(
    real: BigDecimal,
    currencyUnit: CurrencyUnit,
    homeCurrency: CurrencyUnit
) = real.movePointRight(homeCurrency.minorUnitDelta(currencyUnit)).toDouble()

fun CurrencyUnit.minorUnitDelta(other: CurrencyUnit) = fractionDigits - other.fractionDigits