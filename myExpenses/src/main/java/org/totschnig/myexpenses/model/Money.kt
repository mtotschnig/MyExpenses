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

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow

@Parcelize
@Immutable
data class Money(val currencyUnit: CurrencyUnit, val amountMinor: Long) : Parcelable {

    @Throws(ArithmeticException::class)
    constructor(currencyUnit: CurrencyUnit, amountMajor: BigDecimal) :
            this(currencyUnit, convertBigDecimal(amountMajor, currencyUnit.fractionDigits))

    fun negate() = Money(currencyUnit, -amountMinor)

    fun absolute() = if (amountMinor < 0) negate() else this

    val amountMajor: BigDecimal
        get() = BigDecimal(amountMinor).movePointLeft(currencyUnit.fractionDigits)

    companion object {

        fun convertBigDecimal(input: BigDecimal, fractionDigits: Int) = input
            .movePointRight(fractionDigits)
            .setScale(0, RoundingMode.HALF_EVEN)
            .longValueExact()
        /**
         * Builds a Money instance where amount is provided in micro units (=1/1000000 of the main unit)
         *
         * @return a new Money object
         */
        @JvmStatic
        fun buildWithMicros(currency: CurrencyUnit, amountMicros: Long): Money {
            val amountMinor: Long
            val fractionDigits = currency.fractionDigits
            amountMinor = (amountMicros * (10.0).pow(fractionDigits - 6)).toLong()
            return Money(currency, amountMinor)
        }
    }
}

fun Money?.isNullOr0() = this == null || this.amountMinor == 0L