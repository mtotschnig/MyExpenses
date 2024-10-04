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
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.SPLIT_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_ARCHIVE
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS
import org.totschnig.myexpenses.provider.filter.WhereFilter.Operation

abstract class Criterion<T : Any> : Parcelable {
    abstract val operation: Operation
    abstract val values: Array<T>

    open val selectionArgs: Array<String>
        get() = values.map { it.toString() }.toTypedArray()
    abstract val id: Int
    abstract val column: String

    open val key: String
        get() = column

    open val columnForExport: String
        get() = column

    val isNull: Boolean
        get() = operation == Operation.ISNULL

    open fun getSelection(forExport: Boolean): String =
        (if (forExport) columnForExport else column) +
                " " + operation.getOp(selectionArgs.size)

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
        val queryparts = buildList {
            add(selection)
            if (shouldApplyToSplitTransactions) {
                add("($KEY_CATID = $SPLIT_CATID AND exists(select 1 from $tableName children WHERE  $KEY_PARENTID = $tableName.$KEY_ROWID AND ($selection)))")
            }
            if (shouldApplyToArchive) {
                add("($KEY_STATUS = $STATUS_ARCHIVE AND exists(select 1 from $tableName children WHERE $KEY_PARENTID in ($tableName.$KEY_ROWID, (select $KEY_ROWID from $TABLE_TRANSACTIONS grandchildren where $KEY_PARENTID = $tableName.$KEY_ROWID)) AND ($selection)))")
            }
        }
            "($KEY_CATID = $SPLIT_CATID AND exists(select 1 from $tableName children WHERE  $KEY_PARENTID = $tableName.$KEY_ROWID AND ($selection)))"
        "($KEY_STATUS = $STATUS_ARCHIVE AND exists(select 1 from $tableName children WHERE $KEY_PARENTID in ($tableName.$KEY_ROWID, (select $KEY_ROWID from $TABLE_TRANSACTIONS grandchildren where $KEY_PARENTID = $tableName.$KEY_ROWID)) AND ($selection)))"


        return queryparts.joinToString(separator = " OR ", prefix = "(", postfix = ")")
    }

    /**
     * the sums are calculated based on split parts, hence here we must take care to select parts
     * where the parents match
     *
     * @return selection wrapped in a way that is also finds split parts where parents are
     * matched by the criteria
     */
    private fun applyToSplitParents(selection: String, tableName: String): String {
        val selectParents = if (shouldApplyToSplitTransactions) selection else
            "($selection AND $KEY_PARENTID IS NULL)"
        return "($selectParents OR exists(select 1 from $TABLE_TRANSACTIONS parents WHERE $KEY_ROWID = $tableName.$KEY_PARENTID AND ($selection)))"
    }

    fun getSelectionForParts(tableName: String) =
        applyToSplitParents(getSelection(false), tableName)

    fun getSelectionForParents(tableName: String, forExport: Boolean) =
        applyToSplitParts(getSelection(forExport), tableName)

    open val shouldApplyToSplitTransactions get() = true
    open val shouldApplyToArchive get() = true

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