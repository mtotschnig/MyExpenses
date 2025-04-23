package org.totschnig.myexpenses.viewmodel.data

import org.totschnig.myexpenses.retrofit.ExchangeRateSource
import java.math.BigDecimal
import java.time.LocalDate

data class Price(val date: LocalDate, val source: ExchangeRateSource, val value: BigDecimal)

data class FullPrice(
    val date: LocalDate,
    val source: ExchangeRateSource,
    val value: Double,
    val currency: String,
    val commodity: String,
)