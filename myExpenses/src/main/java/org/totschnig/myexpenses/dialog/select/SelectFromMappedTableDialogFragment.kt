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
package org.totschnig.myexpenses.dialog.select

import android.os.Bundle
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.HOME_AGGREGATE_ID
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.filter.Criterion
import kotlin.math.abs

abstract class SelectFromMappedTableDialogFragment<T : Criterion<*>> protected constructor(withNullItem: Boolean) :
    SelectFilterDialog<T>(withNullItem) {
    override val column: String
        get() = DatabaseConstants.KEY_LABEL
    override val selection: String?
        get() = accountSelection(requireArguments().getLong(DatabaseConstants.KEY_ROWID))
    override val selectionArgs: Array<String>?
        get() = accountSelectionArgs(requireArguments().getLong(DatabaseConstants.KEY_ROWID))

    protected fun setArguments(rowId: Long) {
        val args = Bundle(1)
        args.putLong(DatabaseConstants.KEY_ROWID, rowId)
        arguments = args
    }

    companion object {
        fun accountSelection(accountId: Long): String? {
            if (accountId > 0) {
                return DatabaseConstants.KEY_ACCOUNTID + " = ?"
            } else if (accountId != HOME_AGGREGATE_ID) {
                return DatabaseConstants.KEY_ACCOUNTID + " IN " +
                        "(SELECT " + DatabaseConstants.KEY_ROWID + " FROM " + DatabaseConstants.TABLE_ACCOUNTS + " WHERE " + DatabaseConstants.KEY_CURRENCY +
                        " = (SELECT " + DatabaseConstants.KEY_CODE + " FROM " + DatabaseConstants.TABLE_CURRENCIES + " WHERE " + DatabaseConstants.KEY_ROWID + " = ?))"
            }
            return null
        }

        fun accountSelectionArgs(accountId: Long): Array<String>? {
            return if (accountId == HOME_AGGREGATE_ID) null else arrayOf(
                abs(accountId).toString()
            )
        }
    }
}