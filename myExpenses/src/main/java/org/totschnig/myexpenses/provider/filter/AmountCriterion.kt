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
 *
 *   Based on Financisto (c) 2010 Denis Solonenko, made available
 *   under the terms of the GNU Public License v2.0
 */
package org.totschnig.myexpenses.provider.filter

import android.content.Context
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.filter.WhereFilter.Operation
import org.totschnig.myexpenses.util.formatMoney
import kotlin.math.abs

@Parcelize
class AmountCriterion(
    override val operation: Operation,
    override val values: Array<Long>,
    private val currency: String,
    private val type: Boolean,
) : Criterion<Long>() {

    @IgnoredOnParcel
    override val id = R.id.FILTER_AMOUNT_COMMAND

    @IgnoredOnParcel
    override val column = DatabaseConstants.KEY_DISPLAY_AMOUNT

    override val key: String
        get() = DatabaseConstants.KEY_AMOUNT

    override fun prettyPrint(context: Context): String {
        val currencyFormatter = context.injector.currencyFormatter()
        val currencyContext = context.injector.currencyContext()
        val currencyUnit = currencyContext[currency]
        val amount1 = currencyFormatter.formatMoney(Money(currencyUnit, abs(values[0])))
        return context.getString(if (type) R.string.income else R.string.expense) + " " + when (operation) {
            Operation.EQ -> "= $amount1"
            Operation.GTE, Operation.LTE -> "≥ $amount1"
            Operation.BTW -> {
                if (values[1] == 0L) {
                    "<= $amount1"
                } else {
                    val amount2 = currencyFormatter.formatMoney(Money(currencyUnit, abs(values[1])))
                    if (values[0] == 0L) "<=  $amount2" else context.getString(R.string.between_and, amount1, amount2)
                }
            }
            else -> throw IllegalArgumentException()
        }
    }

    override fun toString(): String {
        var result =
            operation.name + EXTRA_SEPARATOR + currency + EXTRA_SEPARATOR + (if (type) "1" else "0") + EXTRA_SEPARATOR + values[0]
        if (operation == Operation.BTW) {
            result += EXTRA_SEPARATOR + values[1]
        }
        return result
    }

    companion object {

        fun create(
            operation: Operation,
            currency: String,
            type: Boolean,
            value1: Long,
            value2: Long?
        ): AmountCriterion {
            val criteriaInfo = transformCriteria(operation, type, value1, value2)
            return AmountCriterion(criteriaInfo.first, criteriaInfo.second, currency, type)
        }

        private fun transformCriteria(
            operation: Operation,
            type: Boolean,
            value1: Long,
            value2: Long?
        ): Pair<Operation, Array<Long>> {
            if (operation !in listOf(Operation.BTW, Operation.EQ, Operation.GTE, Operation.LTE))
                throw UnsupportedOperationException("Operator not supported: " + operation.name)

            val longAmount1: Long = if (type) value1 else -value1
            return when (operation) {
                Operation.BTW -> {
                    if (value2 == null) {
                        throw UnsupportedOperationException("Operator BTW needs two values")
                    }
                    val longAmount2 = if (type) value2 else -value2
                    val needSwap = longAmount2 < longAmount1
                    Operation.BTW to arrayOf(
                        if (needSwap) longAmount2 else longAmount1,
                        if (needSwap) longAmount1 else longAmount2
                    )
                }
                Operation.LTE -> {
                    Operation.BTW to if (type) arrayOf(0, longAmount1) else arrayOf(longAmount1, 0)
                }
                else -> {
                    (if (!type && operation == Operation.GTE) Operation.LTE else operation) to arrayOf(
                        longAmount1
                    )
                }
            }
        }

        fun fromStringExtra(extra: String): AmountCriterion? {
            val values = extra.split(EXTRA_SEPARATOR).toTypedArray()
            return try {
                AmountCriterion(
                    operation = Operation.valueOf(values[0]),
                    currency = values[1],
                    type = values[2] == "1",
                    values = if (Operation.valueOf(values[0]) == Operation.BTW)
                        arrayOf(values[3].toLong(), values[4].toLong())
                    else
                        arrayOf(values[3].toLong())
                )
            } catch (e: NumberFormatException) {
                null
            }
        }
    }
}