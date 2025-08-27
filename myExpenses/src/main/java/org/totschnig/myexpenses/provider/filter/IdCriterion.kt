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
import kotlinx.serialization.Serializable
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.crashreporting.CrashHandler.Companion.report

@Serializable
sealed class IdCriterion : SimpleCriterion<Long>() {

    abstract val label : String

    final override val operation
        get() = if (values.isEmpty()) Operation.IS_NULL else Operation.IN

    override fun prettyPrint(context: Context): String {
        return if (operation == Operation.IS_NULL) context.getString(R.string.unmapped) else label
    }

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