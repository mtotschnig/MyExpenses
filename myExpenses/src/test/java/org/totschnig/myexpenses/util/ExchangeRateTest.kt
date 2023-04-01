package org.totschnig.myexpenses.util

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.*

import org.junit.Test
import org.totschnig.myexpenses.model.CurrencyUnit
import java.util.*

class ExchangeRateTest {

    @Test
    fun testExchangeRateCalculations() {
        val eur = CurrencyUnit("EUR", "€", 2)
        val jpy = CurrencyUnit("YPY", "¥", 0)
        assertThat(calculateRawExchangeRate(0.0069, jpy, eur)).isEqualTo(0.69)
        assertThat(calculateRealExchangeRate(0.69, jpy, eur)).isEqualTo(0.0069)
        assertThat(calculateRawExchangeRate(144.34, eur, jpy)).isEqualTo(1.4434)
        assertThat(calculateRealExchangeRate(1.4434, eur, jpy)).isEqualTo(144.34)
    }
}