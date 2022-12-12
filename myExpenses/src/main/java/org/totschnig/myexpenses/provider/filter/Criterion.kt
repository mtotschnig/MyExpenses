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
import android.os.Parcelable
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.filter.WhereFilter.Operation

abstract class Criterion<T: Any> : Parcelable {
    abstract val operation: Operation
    abstract val values: Array<T>

    open val selectionArgs: Array<String>
        get() = values.map { it.toString() }.toTypedArray()
    abstract val id: Int
    abstract val column: String


    val isNull: Boolean
        get() = operation == Operation.ISNULL
    open val selection: String
        get() = column + " " + operation.getOp(selectionArgs.size)

    fun size(): Int = values.size

    open fun prettyPrint(context: Context) = values.joinToString()

    override fun toString(): String {
        throw UnsupportedOperationException("Only subclasses can be persisted")
    }

    /**
     * @return selection wrapped in a way that it also finds split transactions with parts
     * that are matched by the criteria
     */
    private fun applyToSplitParts(selection: String, tableName: String): String {
        return if (!shouldApplyToParts()) {
            selection
        } else "(" + selection + " OR (" + DatabaseConstants.KEY_CATID + " = " + DatabaseConstants.SPLIT_CATID +
                " AND exists(select 1 from " + DatabaseConstants.TABLE_TRANSACTIONS + " children"+
                " WHERE " + DatabaseConstants.KEY_PARENTID +
                " = " + tableName + "." + DatabaseConstants.KEY_ROWID + " AND (" + selection + "))))"
    }

    /**
     * the sums are calculated based on split parts, hence here we must take care to select parts
     * where the parents match
     *
     * @return selection wrapped in a way that is also finds split parts where parents are
     * matched by the criteria
     */
    private fun applyToSplitParents(selection: String, tableName: String): String {
        val selectParents = if (!shouldApplyToParts()) {
            "(" + selection + " AND " + DatabaseConstants.KEY_PARENTID + " IS NULL)"
        } else {
            selection
        }
        return ("(" + selectParents + " OR  exists(select 1 from " + DatabaseConstants.TABLE_TRANSACTIONS + " parents"
                + " WHERE " + DatabaseConstants.KEY_ROWID
                + " = " + tableName + "." + DatabaseConstants.KEY_PARENTID + " AND (" + selection + ")))")
    }

    fun getSelectionForParts(tableName: String) = applyToSplitParents(selection, tableName)

    fun getSelectionForParents(tableName: String) = applyToSplitParts(selection, tableName)

    open fun shouldApplyToParts() = true

    companion object {
        const val EXTRA_SEPARATOR = ";"
        val EXTRA_SEPARATOR_ESCAPE_SAVE_REGEXP = "(?<!\\\\);".toRegex()
        @JvmStatic
        fun escapeSeparator(`in`: String): String {
            return `in`.replace(";", "\\;")
        }

        @JvmStatic
        fun unescapeSeparator(`in`: String): String {
            return `in`.replace("\\;", ";")
        }
    }
}