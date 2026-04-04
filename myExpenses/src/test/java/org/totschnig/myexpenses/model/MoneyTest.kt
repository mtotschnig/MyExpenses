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
import junit.framework.TestCase
import java.math.BigDecimal
import java.util.Currency
import kotlin.math.pow

const val DEFAULT_FRACTION_DIGITS: Int = 8

class MoneyTest : TestCase() {
    lateinit var currencyUnit: CurrencyUnit
    lateinit var money: Money

    /**
     * test a Currency with 2 FractionDigits
     */
    fun testEUR() {
        currencyUnit = buildCurrencyUnit("EUR")
        money = Money(currencyUnit, 2345L)
        assertThat(money.amountMinor).isEqualTo(2345L)
        assertThat(money.amountMajor).isEqualTo(BigDecimal("23.45"))
        money = Money.buildWithMajor(currencyUnit, BigDecimal("34.56")).getOrThrow()
        assertThat(money.amountMinor).isEqualTo(3456L)
    }

    /**
     * test a Currency with 3 FractionDigits
     */
    fun testBHD() {
        currencyUnit = buildCurrencyUnit("BHD")
        money = Money(currencyUnit, 2345L)
        assertThat(money.amountMinor).isEqualTo(2345L)
        assertThat(money.amountMajor).isEqualTo(BigDecimal("2.345"))
        money = Money.buildWithMajor(currencyUnit, BigDecimal("3.456")).getOrThrow()
        assertThat(money.amountMinor).isEqualTo(3456L)
    }

    /**
     * test a Currency with 0 FractionDigits
     */
    fun testJPY() {
        currencyUnit = buildCurrencyUnit("JPY")
        money = Money(currencyUnit, 2345L)
        assertThat(money.amountMinor).isEqualTo(2345L)
        assertThat(money.amountMajor).isEqualTo(BigDecimal("2345"))
        money = Money.buildWithMajor(currencyUnit, BigDecimal("3456")).getOrThrow()
        assertThat(money.amountMinor).isEqualTo(3456L)
    }

    /**
     * test no Currency
     */
    fun testXXX() {
        currencyUnit = buildXXX()
        val factor = 10.0.pow(DEFAULT_FRACTION_DIGITS)
        val minor = (2345 * factor).toLong()
        money = Money(currencyUnit, minor)
        assertThat(money.amountMinor).isEqualTo(minor)
        assertThat(money.amountMajor).isEquivalentAccordingToCompareTo(BigDecimal("2345"))
        money = Money.buildWithMajor(currencyUnit, BigDecimal("3456.789")).getOrThrow()
        assertThat(money.amountMinor).isEqualTo((3456.789 * factor).toLong())
    }

    fun testBuildWithMicrosEUR() {
        currencyUnit = buildCurrencyUnit("EUR")
        assertThat(currencyUnit.fractionDigits).isEqualTo(2)
        money = Money.buildWithMicros(currencyUnit, 23450000)
        assertThat(money.amountMinor).isEqualTo(2345L)
    }

    fun testBuildWithMicrosBHD() {
        currencyUnit = buildCurrencyUnit("BHD")
        assertThat(currencyUnit.fractionDigits).isEqualTo(3)
        money = Money.buildWithMicros(currencyUnit, 23450000)
        assertThat(money.amountMinor).isEqualTo(23450L)
    }

    fun testBuildWithMicrosJPY() {
        currencyUnit = buildCurrencyUnit("JPY")
        assertThat(currencyUnit.fractionDigits).isEqualTo(0)
        money = Money.buildWithMicros(currencyUnit, 23450000)
        assertThat(money.amountMinor).isEqualTo(23L)
    }

    fun testBuildWithMicrosXXX() {
        currencyUnit = buildXXX()
        assertThat(currencyUnit.fractionDigits).isEqualTo(DEFAULT_FRACTION_DIGITS)
        money = Money.buildWithMicros(currencyUnit, 23450000)
        assertThat(money.amountMinor).isEqualTo(2345000000L)
    }

    private fun buildCurrencyUnit(code: String): CurrencyUnit {
        val currency = Currency.getInstance(code)
        return CurrencyUnit(code, currency.symbol, currency.getDefaultFractionDigits())
    }

    private fun buildXXX(): CurrencyUnit {
        return CurrencyUnit("XXX", "XXX", DEFAULT_FRACTION_DIGITS)
    }
}
