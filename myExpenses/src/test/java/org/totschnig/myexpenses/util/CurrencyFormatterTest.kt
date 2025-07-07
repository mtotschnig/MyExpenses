package org.totschnig.myexpenses.util

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.prefHandler
import org.totschnig.myexpenses.preference.PrefKey
import java.util.*

class CurrencyFormatterTest {
    //TODO extend
    private lateinit var currencyFormatter: CurrencyFormatter
    private lateinit var application: MyApplication

    @Before
    fun setUp() {
        whenever(prefHandler.getString(eq(PrefKey.CUSTOM_DECIMAL_FORMAT), any())).thenReturn("")
        application = mock()
        currencyFormatter = CurrencyFormatter(prefHandler, application)
    }

    @Test
    fun testMoneyFormatGermany() {
        val eur = CurrencyUnit("EUR", "€", 2)
        whenever(application.userPreferredLocale).thenReturn(Locale.GERMANY)

        val javaVersion = System.getProperty("java.version")!!.split('.')[0].toInt()
        //newer Java version uses non-breaking space
        assertThat(currencyFormatter.formatMoney(Money(eur, 150))).isEqualTo(if (javaVersion >= 10) "1,50 €" else "1,50 €")
    }
}