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
import kotlin.math.absoluteValue

@Parcelize
class AmountCriterion(
    override val operation: Operation,
    override val values: Array<Long>,
    private val currency: String,
    val type: Boolean,
) : Criterion<Long>() {

    init {
        if (type) {
            require(values.all { it >= 0 })
        } else {
            require(values.all { it <= 0 })
        }
    }

    @IgnoredOnParcel
    override val id = R.id.FILTER_AMOUNT_COMMAND

    @IgnoredOnParcel
    override val column = DatabaseConstants.KEY_DISPLAY_AMOUNT

    override val columnForExport: String
        get() = DatabaseConstants.KEY_AMOUNT

    override val key: String
        get() = DatabaseConstants.KEY_AMOUNT

    override fun prettyPrint(context: Context): String {
        val currencyFormatter = context.injector.currencyFormatter()
        val currencyContext = context.injector.currencyContext()
        val currencyUnit = currencyContext[currency]
        val transformed = transformForUi()
        val amount1 = currencyFormatter.formatMoney(Money(currencyUnit, transformed.second[0]))
        return context.getString(if (type) R.string.income else R.string.expense) + " " +
                when (transformed.first) {
                    Operation.EQ -> "= $amount1"
                    Operation.GTE -> "≥ $amount1"
                    Operation.LTE -> "<= $amount1"
                    Operation.BTW -> context.getString(
                        R.string.between_and,
                        amount1,
                        currencyFormatter.formatMoney(Money(currencyUnit, transformed.second[1]))
                    )

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

    fun transformForUi(): Pair<Operation, Array<Long>> {
        val value1 = values[0].absoluteValue
        val value2 = values.getOrNull(1)?.absoluteValue
        return when (operation) {
            Operation.EQ, Operation.GTE -> operation to arrayOf(value1)
            Operation.LTE -> if (!type) Operation.GTE to arrayOf(value1)
            else throw IllegalStateException("LTE for income not expected")

            Operation.BTW -> when {
                value1 == 0L -> Operation.LTE to arrayOf(value2!!)
                value2 == 0L -> Operation.LTE to arrayOf(value1)
                else -> operation to if (type) arrayOf(value1, value2!!) else arrayOf(
                    value2!!,
                    value1
                )
            }

            else -> throw IllegalStateException("Operator not expected: " + operation.name)
        }
    }

    companion object {

        fun create(
            operation: Operation,
            currency: String,
            type: Boolean,
            value1: Long,
            value2: Long?
        ): AmountCriterion {
            val criteriaInfo = transformForStorage(operation, type, value1, value2)
            return AmountCriterion(criteriaInfo.first, criteriaInfo.second, currency, type)
        }

        private fun transformForStorage(
            operation: Operation,
            type: Boolean,
            value1: Long,
            value2: Long?
        ): Pair<Operation, Array<Long>> {
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

                Operation.LTE ->
                    Operation.BTW to if (type) arrayOf(0, longAmount1) else arrayOf(longAmount1, 0)

                Operation.GTE -> {
                    (if (type) Operation.GTE else Operation.LTE) to arrayOf(longAmount1)
                }

                Operation.EQ -> operation to arrayOf(longAmount1)
                else -> throw UnsupportedOperationException("Operator not supported: " + operation.name)
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