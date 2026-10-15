/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.totschnig.myexpenses.model

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.preference.PrefHandler
import java.util.Currency

class DatabaseCurrencyContextTest {

    private lateinit var currencyContext: DatabaseCurrencyContext
    private lateinit var prefHandler: PrefHandler
    private lateinit var application: MyApplication

    @Before
    fun setUp() {
        prefHandler = mock()
        application = mock()
        whenever(application.contentResolver).thenReturn(mock())
        currencyContext = DatabaseCurrencyContext(prefHandler, application)
    }

    @Test
    fun testGetCurrencyForCountryBulgariaReturnsEuro() {
        val currencyUppercase = currencyContext.getCurrencyForCountry("BG")
        assertThat(currencyUppercase).isEqualTo(Currency.getInstance("EUR"))

        val currencyLowercase = currencyContext.getCurrencyForCountry("bg")
        assertThat(currencyLowercase).isEqualTo(Currency.getInstance("EUR"))
    }

    @Test
    fun testGetCurrencyForCountryOtherCountries() {
        val usCurrency = currencyContext.getCurrencyForCountry("US")
        assertThat(usCurrency).isEqualTo(Currency.getInstance("USD"))

        val deCurrency = currencyContext.getCurrencyForCountry("DE")
        assertThat(deCurrency).isEqualTo(Currency.getInstance("EUR"))
    }
}
