package org.totschnig.myexpenses.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import java.util.*

class CurrencyFormatterTest {
    //TODO extend
    private lateinit var currencyFormatter: CurrencyFormatter
    private lateinit var userLocaleProvider: UserLocaleProvider

    @Before
    fun setUp() {
        val preHandler = Mockito.mock(PrefHandler::class.java)
        Mockito.`when`(preHandler.getString(ArgumentMatchers.eq(PrefKey.CUSTOM_DECIMAL_FORMAT), any())).thenReturn("")
        userLocaleProvider = Mockito.mock(UserLocaleProvider::class.java)
        currencyFormatter = CurrencyFormatter(preHandler, userLocaleProvider)
    }

    @Test
    fun testMoneyFormatGermany() {
        val eur = CurrencyUnit("EUR", "€", 2)
        Mockito.`when`(userLocaleProvider.getUserPreferredLocale()).thenReturn(Locale.GERMANY)

        val javaVersion = System.getProperty("java.version")!!.split('.')[0].toInt()
        //newer Java version uses non-breaking space
        assertThat(currencyFormatter.formatCurrency(Money(eur, 150))).isEqualTo(if (javaVersion >= 10) "1,50 €" else "1,50 €")
    }
}