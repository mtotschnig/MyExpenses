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
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.toEndOfDayEpoch
import org.totschnig.myexpenses.util.toStartOfDayEpoch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Parcelize
class DateCriterion(
    override val operation: WhereFilter.Operation,
    override val values: Array<LocalDate>
) : Criterion<LocalDate>() {
    /**
     * filters transactions up to or from the provided value, depending on operation
     *
     * @param operation either [WhereFilter.Operation.LTE] or [WhereFilter.Operation.GTE]
     */
    constructor(operation: WhereFilter.Operation, value1: LocalDate) :
            this(operation, arrayOf(value1))

    /**
     * filters transaction between the provided values
     */
    constructor(value1: LocalDate, value2: LocalDate) :
            this(WhereFilter.Operation.BTW, arrayOf(value1, value2))

    @IgnoredOnParcel
    override val id = R.id.FILTER_DATE_COMMAND
    @IgnoredOnParcel
    override val column = DatabaseConstants.KEY_DATE

    override fun toString(): String {
        var result: String = operation.name + EXTRA_SEPARATOR + values[0]
        if (operation === WhereFilter.Operation.BTW) {
            result += EXTRA_SEPARATOR + values[1]
        }
        return result
    }

    override val selectionArgs: Array<String>
        get() = when (operation) {
            WhereFilter.Operation.GTE, WhereFilter.Operation.LT -> arrayOf(toStartOfDay(values[0]))
            WhereFilter.Operation.LTE, WhereFilter.Operation.GT -> arrayOf(toEndOfDay(values[0]))
            WhereFilter.Operation.BTW -> arrayOf(toStartOfDay(values[0]), toEndOfDay(values[1]))
            else -> throw IllegalStateException("Unexpected value: $operation")
        }

    private fun toStartOfDay(localDate: LocalDate) = localDate.toStartOfDayEpoch().toString()

    private fun toEndOfDay(localDate: LocalDate) = localDate.toEndOfDayEpoch().toString()

    override fun prettyPrint(context: Context): String {
        var result = ""
        val df = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
        val date1 = df.format(values[0])
        when (operation) {
            WhereFilter.Operation.GTE, WhereFilter.Operation.GT -> result = context.getString(R.string.after, date1)
            WhereFilter.Operation.LTE, WhereFilter.Operation.LT -> result = context.getString(R.string.before, date1)
            WhereFilter.Operation.BTW -> {
                val date2 = df.format(values[1])
                result += context.getString(R.string.between_and, date1, date2)
            }
            else -> throw IllegalStateException("Unexpected value: $operation")
        }
        return result
    }

    override fun shouldApplyToParts(): Boolean {
        return false
    }

    companion object {

        fun fromStringExtra(extra: String): DateCriterion {
            val values = extra.split(EXTRA_SEPARATOR).toTypedArray()
            val op = WhereFilter.Operation.valueOf(values[0])
            return if (op == WhereFilter.Operation.BTW) {
                DateCriterion(
                    LocalDate.parse(values[1]),
                    LocalDate.parse(values[2])
                )
            } else DateCriterion(
                op,
                LocalDate.parse(values[1])
            )
        }

        fun fromLegacy(extra: String): DateCriterion {
            val values = extra.split(EXTRA_SEPARATOR).toTypedArray()
            val op = WhereFilter.Operation.valueOf(values[0])
            return if (op == WhereFilter.Operation.BTW) {
                DateCriterion(
                    fromEpoch(values[1]),
                    fromEpoch(values[2])
                )
            } else DateCriterion(op, fromEpoch(values[1]))
        }

        private fun fromEpoch(epoch: String): LocalDate {
            return Instant.ofEpochSecond(epoch.toLong()).atZone(ZoneId.systemDefault())
                .toLocalDate()
        }
    }
}