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
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CODE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CURRENCIES
import org.totschnig.myexpenses.provider.filter.IdCriterion
import org.totschnig.myexpenses.provider.filter.KEY_CRITERION
import kotlin.math.abs

abstract class SelectFromMappedTableDialogFragment<T : IdCriterion>(
    withNullItem: Boolean,
    typeParameterClass: Class<T>,
) : SelectFilterDialog<T>(withNullItem, typeParameterClass) {
    override val column: String
        get() = DatabaseConstants.KEY_LABEL
    override val selection: String?
        get() = accountSelection(requireArguments().getLong(KEY_ROWID))
    override val selectionArgs: Array<String>?
        get() = accountSelectionArgs(requireArguments().getLong(KEY_ROWID))

    protected fun configureArguments(requestKey: String, rowId: Long, criterion: T?) =
        configureArguments(requestKey)
            .apply {
                putLong(KEY_ROWID, rowId)
                putParcelable(KEY_CRITERION, criterion)
            }

    companion object {
        fun accountSelection(accountId: Long) =
            if (accountId > 0) "$KEY_ACCOUNTID = ?"
            else if (accountId != HOME_AGGREGATE_ID) {
                KEY_ACCOUNTID + " IN " +
                        "(SELECT " + KEY_ROWID + " FROM " + TABLE_ACCOUNTS + " WHERE " + KEY_CURRENCY +
                        " = (SELECT " + KEY_CODE + " FROM " + TABLE_CURRENCIES + " WHERE " + KEY_ROWID + " = ?))"
            } else null

        fun accountSelectionArgs(accountId: Long): Array<String>? {
            return if (accountId == HOME_AGGREGATE_ID) null else arrayOf(
                abs(accountId).toString()
            )
        }
    }
}