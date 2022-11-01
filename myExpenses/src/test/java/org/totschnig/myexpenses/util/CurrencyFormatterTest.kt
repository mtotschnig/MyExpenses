package org.totschnig.myexpenses.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.prefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import java.util.*

class CurrencyFormatterTest {
    //TODO extend
    private lateinit var currencyFormatter: CurrencyFormatter
    private lateinit var userLocaleProvider: UserLocaleProvider

    @Before
    fun setUp() {
        whenever(prefHandler.getString(eq(PrefKey.CUSTOM_DECIMAL_FORMAT), any())).thenReturn("")
        userLocaleProvider = mock()
        currencyFormatter = CurrencyFormatter(prefHandler, userLocaleProvider)
    }

    @Test
    fun testMoneyFormatGermany() {
        val eur = CurrencyUnit("EUR", "€", 2)
        whenever(userLocaleProvider.getUserPreferredLocale()).thenReturn(Locale.GERMANY)

        val javaVersion = System.getProperty("java.version")!!.split('.')[0].toInt()
        //newer Java version uses non-breaking space
        assertThat(currencyFormatter.formatMoney(Money(eur, 150))).isEqualTo(if (javaVersion >= 10) "1,50 €" else "1,50 €")
    }
}