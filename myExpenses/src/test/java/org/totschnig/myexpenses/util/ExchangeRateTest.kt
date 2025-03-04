package org.totschnig.myexpenses.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.totschnig.myexpenses.model.CurrencyUnit
import java.math.BigDecimal

class ExchangeRateTest {

    @Test
    fun testExchangeRateCalculations() {
        val eur = CurrencyUnit("EUR", "€", 2)
        val jpy = CurrencyUnit("YPY", "¥", 0)
        assertThat(calculateRawExchangeRate(BigDecimal.valueOf(0.0069), jpy, eur)).isEqualTo(0.69)
        assertThat(calculateRealExchangeRate(0.69, jpy, eur)).isEqualToIgnoringScale(BigDecimal.valueOf(0.0069))
        assertThat(calculateRawExchangeRate(BigDecimal.valueOf(144.34), eur, jpy)).isEqualTo(1.4434)
        assertThat(calculateRealExchangeRate(1.4434, eur, jpy)).isEqualToIgnoringScale(BigDecimal.valueOf(144.34))
    }
}