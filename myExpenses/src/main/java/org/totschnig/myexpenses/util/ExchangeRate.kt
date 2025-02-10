package org.totschnig.myexpenses.util

import org.totschnig.myexpenses.model.CurrencyUnit
import java.math.BigDecimal
import kotlin.math.pow

fun calculateRealExchangeRate(
    raw: Double,
    currencyUnit: CurrencyUnit,
    homeCurrency: CurrencyUnit
) = BigDecimal.valueOf(raw).movePointRight(currencyUnit.minorUnitDelta(homeCurrency))

fun calculateRawExchangeRate(
    real: BigDecimal,
    currencyUnit: CurrencyUnit,
    homeCurrency: CurrencyUnit
) = real.movePointRight(homeCurrency.minorUnitDelta(currencyUnit)).toDouble()

fun CurrencyUnit.minorUnitDelta(other: CurrencyUnit) = fractionDigits - other.fractionDigits