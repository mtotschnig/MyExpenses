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
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.filter.WhereFilter.Operation
import org.totschnig.myexpenses.util.crashreporting.CrashHandler.Companion.report

abstract class IdCriterion : Criterion<Long>() {

    abstract val label : String?

    override fun prettyPrint(context: Context): String {
        return if (operation == Operation.ISNULL) String.format(
            "%s: %s",
            columnName2Label(context),
            context.getString(R.string.unmapped)
        ) else label!!
    }

    private fun columnName2Label(context: Context): String {
        when (column) {
            DatabaseConstants.KEY_CATID -> return context.getString(R.string.category)
            DatabaseConstants.KEY_PAYEEID -> return context.getString(R.string.payer_or_payee)
            DatabaseConstants.KEY_METHODID -> return context.getString(R.string.method)
        }
        return column
    }

    override fun toString() =
        if (operation == Operation.ISNULL) "null" else escapeSeparator(label!!) +
                EXTRA_SEPARATOR + values.joinToString(EXTRA_SEPARATOR)

    companion object {

        fun parseStringExtra(extra: String): Pair<String, LongArray>? {
            val extraParts = extra.split(EXTRA_SEPARATOR_ESCAPE_SAVE_REGEXP).toTypedArray()
            if (extraParts.size >= 2) {
                val map = extraParts.copyOfRange(1, extraParts.size).map { it.toLongOrNull() }
                if (!map.contains(null)) {
                    return unescapeSeparator(extraParts[0]) to map.filterNotNull().toLongArray()
                }
            }
            reportCannotParse(extra)
            return null
        }

        private fun reportCannotParse(extra: String) {
            report(Exception("Cannot parse string extra $extra"))
        }
    }
}