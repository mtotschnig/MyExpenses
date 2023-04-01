package org.totschnig.myexpenses.util

import org.totschnig.myexpenses.model.CurrencyUnit
import kotlin.math.pow

fun calculateRealExchangeRate(
    raw: Double,
    currencyUnit: CurrencyUnit,
    homeCurrency: CurrencyUnit
) = raw * 10.0.pow(currencyUnit.minorUnitDelta(homeCurrency))

fun calculateRawExchangeRate(
    real: Double,
    currencyUnit: CurrencyUnit,
    homeCurrency: CurrencyUnit
) = real * 10.0.pow(homeCurrency.minorUnitDelta(currencyUnit))

fun CurrencyUnit.minorUnitDelta(other: CurrencyUnit) = fractionDigits - other.fractionDigits