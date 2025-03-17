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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Today
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.toEndOfDayEpoch
import org.totschnig.myexpenses.util.toStartOfDayEpoch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object LocalDateSerializer : KSerializer<LocalDate> {
    // Serial names of descriptors should be unique, so choose app-specific name in case some library also would declare a serializer for Date.
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("org.totschnig.myexpenses.provider.filter.LocalDate", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}

@Parcelize
@Serializable
@SerialName(DatabaseConstants.KEY_DATE)
data class DateCriterion(
    override val operation: Operation,
    override val values: List<@Serializable(with = LocalDateSerializer::class) LocalDate>
) : SimpleCriterion<@Serializable(with = LocalDateSerializer::class) LocalDate>() {
    /**
     * filters transactions up to or from the provided value, depending on operation
     *
     * @param operation either [Operation.LTE] or [Operation.GTE]
     */
    constructor(operation: Operation, value1: LocalDate) :
            this(operation, listOf(value1))

    /**
     * filters transaction between the provided values
     */
    constructor(value1: LocalDate, value2: LocalDate) :
            this(Operation.BTW, listOf(value1, value2))

    @IgnoredOnParcel
    override val id = R.id.FILTER_DATE_COMMAND
    @IgnoredOnParcel
    override val column = DatabaseConstants.KEY_DATE

    override val displayInfo: DisplayInfo
        get() = DateCriterion

    override val selectionArgs: Array<String>
        get() = when (operation) {
            Operation.GTE, Operation.LT -> arrayOf(toStartOfDay(values[0]))
            Operation.LTE, Operation.GT -> arrayOf(toEndOfDay(values[0]))
            Operation.BTW -> arrayOf(toStartOfDay(values[0]), toEndOfDay(values[1]))
            else -> throw IllegalStateException("Unexpected value: $operation")
        }

    private fun toStartOfDay(localDate: LocalDate) = localDate.toStartOfDayEpoch().toString()

    private fun toEndOfDay(localDate: LocalDate) = localDate.toEndOfDayEpoch().toString()

    override fun prettyPrint(context: Context): String {
        var result = ""
        val df = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
        val date1 = df.format(values[0])
        when (operation) {
            Operation.GTE, Operation.GT -> result = context.getString(R.string.after, date1)
            Operation.LTE, Operation.LT -> result = context.getString(R.string.before, date1)
            Operation.BTW -> {
                val date2 = df.format(values[1])
                result += context.getString(R.string.between_and, date1, date2)
            }
            else -> throw IllegalStateException("Unexpected value: $operation")
        }
        return result
    }

    override val shouldApplyToSplitTransactions get() = false
    override val shouldApplyToArchive: Boolean get() = false

    companion object: DisplayInfo {

        fun fromStringExtra(extra: String): DateCriterion {
            val values = extra.split(EXTRA_SEPARATOR).toTypedArray()
            val op = Operation.valueOf(values[0])
            return if (op == Operation.BTW) {
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
            val op = Operation.valueOf(values[0])
            return if (op == Operation.BTW) {
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

        override val title = R.string.date
        override val extendedTitle = R.string.search_date
        override val icon = Icons.Default.Today
        override val clazz = DateCriterion::class
    }
}