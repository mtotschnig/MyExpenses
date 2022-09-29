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
import android.text.TextUtils
import org.totschnig.myexpenses.provider.filter.Criteria.operation
import org.totschnig.myexpenses.provider.filter.Criteria.column
import org.totschnig.myexpenses.provider.filter.Criteria.Companion.escapeSeparator
import org.totschnig.myexpenses.provider.filter.Criteria.selectionArgs
import org.totschnig.myexpenses.util.crashreporting.CrashHandler.Companion.report
import org.totschnig.myexpenses.provider.filter.Criteria.Companion.unescapeSeparator
import org.totschnig.myexpenses.provider.filter.IdCriteria
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import java.lang.Exception
import java.lang.RuntimeException
import java.util.*

abstract class IdCriteria : Criteria {
    val label: String?

    internal constructor(label: String?, vararg ids: Long) : this(
        label,
        *longArrayToStringArray(ids)
    ) {
    }

    internal constructor(label: String?, vararg ids: String?) : super(
        WhereFilter.Operation.IN,
        ids
    ) {
        this.label = label
    }

    internal constructor() : super(WhereFilter.Operation.ISNULL, arrayOf<String>()) {
        label = null
    }

    override fun prettyPrint(context: Context?): String? {
        return if (operation == WhereFilter.Operation.ISNULL) String.format(
            "%s: %s",
            columnName2Label(context),
            context!!.getString(R.string.unmapped)
        ) else label
    }

    private fun columnName2Label(context: Context?): String {
        when (column) {
            DatabaseConstants.KEY_CATID -> return context!!.getString(R.string.category)
            DatabaseConstants.KEY_PAYEEID -> return context!!.getString(R.string.payer_or_payee)
            DatabaseConstants.KEY_METHODID -> return context!!.getString(R.string.method)
        }
        return column
    }

    override fun toStringExtra(): String? {
        return if (operation == WhereFilter.Operation.ISNULL) "null" else escapeSeparator(
            label!!
        ) + EXTRA_SEPARATOR + TextUtils.join(EXTRA_SEPARATOR, selectionArgs)
    }

    companion object {
        protected fun longArrayToStringArray(`in`: LongArray): Array<String?> {
            val out = arrayOfNulls<String>(`in`.size)
            for (i in `in`.indices) {
                out[i] = `in`[i].toString()
            }
            return out
        }

        fun <T : IdCriteria?> fromStringExtra(extra: String, clazz: Class<T>): T? {
            val extraParts = extra.split(EXTRA_SEPARATOR_ESCAPE_SAVE_REGEXP).toTypedArray()
            if (extraParts.size < 2) {
                report(
                    Exception(
                        String.format(
                            "Unparsable string extra %s for %s",
                            Arrays.toString(extraParts),
                            clazz.name
                        )
                    )
                )
                return null
            }
            val ids = Arrays.copyOfRange(extraParts, 1, extraParts.size)
            val label = unescapeSeparator(
                extraParts[0]
            )
            return try {
                clazz.getConstructor(String::class.java, Array<String>::class.java)
                    .newInstance(label, ids)
            } catch (e: Exception) {
                throw RuntimeException("Unable to find constructor for class " + clazz.name)
            }
        }
    }
}