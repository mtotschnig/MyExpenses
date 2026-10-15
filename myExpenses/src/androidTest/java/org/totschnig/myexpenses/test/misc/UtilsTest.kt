package org.totschnig.myexpenses.test.misc

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.DatabaseCurrencyContext
import org.totschnig.myexpenses.util.PdfHelper
import org.totschnig.myexpenses.util.PdfHelper.Companion.hasAnyRtl
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.Utils.validateNumber
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency

@RunWith(AndroidJUnit4::class)
class UtilsTest {

    @Test
    fun testValidateNumber() {
        var symbols = DecimalFormatSymbols().apply {
            decimalSeparator = '.'
        }
        var nfDLocal = DecimalFormat("#0.###", symbols)
        assertThat(validateNumber(nfDLocal, "4.7")).isEqualTo(BigDecimal("4.7"))
        assertThat(validateNumber(nfDLocal, "4,7")).isNull()

        symbols = DecimalFormatSymbols().apply {
            decimalSeparator = ','
        }
        nfDLocal = DecimalFormat("#0.###", symbols)
        assertThat(validateNumber(nfDLocal, "4,7")).isEqualTo(BigDecimal("4.7"))
        assertThat(validateNumber(nfDLocal, "4.7")).isNull()

        nfDLocal = DecimalFormat("#0").apply {
            isParseIntegerOnly = true
        }
        assertThat(validateNumber(nfDLocal, "470")).isEqualTo(BigDecimal(470))
        assertThat(validateNumber(nfDLocal, "470.123")).isNull()
    }

    @Test
    fun testPdfHelper() {
        assertThat(hasAnyRtl("test")).isFalse()
        assertThat(hasAnyRtl("مصروفاتي")).isTrue()
        assertThat(hasAnyRtl("הנושאים שלי")).isTrue()
    }

    @Test
    fun testGetCurrencyForCountryBulgariaReturnsEuro() {
        val currencyContext = getInstrumentation().targetContext.injector.currencyContext() as DatabaseCurrencyContext
        val currencyUppercase = currencyContext.getCurrencyForCountry("BG")
        assertThat(currencyUppercase).isEqualTo(Currency.getInstance("EUR"))

        val currencyLowercase = currencyContext.getCurrencyForCountry("bg")
        assertThat(currencyLowercase).isEqualTo(Currency.getInstance("EUR"))
    }
}
