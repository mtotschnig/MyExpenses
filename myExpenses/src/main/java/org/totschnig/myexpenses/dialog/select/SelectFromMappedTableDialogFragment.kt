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

import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.KEY_ACCOUNT_GROUPING
import org.totschnig.myexpenses.model.KEY_ACCOUNT_GROUPING_GROUP
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.HOME_AGGREGATE_ID
import org.totschnig.myexpenses.provider.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.KEY_CODE
import org.totschnig.myexpenses.provider.KEY_CURRENCY
import org.totschnig.myexpenses.provider.KEY_FLAG
import org.totschnig.myexpenses.provider.KEY_LABEL
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.KEY_TYPE
import org.totschnig.myexpenses.provider.TABLE_ACCOUNTS
import org.totschnig.myexpenses.provider.TABLE_CURRENCIES
import org.totschnig.myexpenses.provider.filter.IdCriterion
import org.totschnig.myexpenses.provider.filter.KEY_CRITERION
import org.totschnig.myexpenses.viewmodel.data.PageAccount
import kotlin.math.abs

abstract class SelectFromMappedTableDialogFragment<T : IdCriterion>(
    withNullItem: Boolean,
    typeParameterClass: Class<T>,
) : SelectFilterDialog<T>(withNullItem, typeParameterClass) {
    override val column: String
        get() = KEY_LABEL
    override val selection: String?
        get() {
            val args = requireArguments()
            val accountID = args.getLong(KEY_ROWID)
            return if (accountID == 0L) {
                accountSelectionV2(AccountGrouping.valueOf(args.getString(KEY_ACCOUNT_GROUPING)!!))
            } else accountSelection(accountID).takeIf { it.isNotEmpty() }
        }
    override val selectionArgs: Array<String>?
        get() {
            val args = requireArguments()
            val accountID = args.getLong(KEY_ROWID)
            return if (accountID == 0L) {
                accountSelectionArgsV2(
                    AccountGrouping.valueOf(args.getString(KEY_ACCOUNT_GROUPING)!!),
                    args.getString(KEY_ACCOUNT_GROUPING_GROUP)!!
                    )
            } else accountSelectionArgs(requireArguments().getLong(KEY_ROWID))
        }

    protected fun configureArguments(requestKey: String, account: PageAccount, criterion: T?) =
        configureArguments(requestKey)
            .apply {
                if (account.id == 0L) {
                    putString(KEY_ACCOUNT_GROUPING, account.accountGrouping!!.name)
                    putString(
                        KEY_ACCOUNT_GROUPING_GROUP,
                        when (account.accountGrouping) {
                            AccountGrouping.CURRENCY -> account.currency
                            AccountGrouping.FLAG -> account.flag.id.toString()
                            AccountGrouping.TYPE -> account.type.id.toString()
                            else -> "Unit"
                        }
                    )
                }
                putLong(KEY_ROWID, account.id)
                putParcelable(KEY_CRITERION, criterion)
            }

    companion object {
        fun accountSelection(accountId: Long) =
            if (accountId > 0) "$KEY_ACCOUNTID = ?"
            else if (accountId != HOME_AGGREGATE_ID) {
                "$KEY_ACCOUNTID IN (SELECT $KEY_ROWID FROM $TABLE_ACCOUNTS WHERE $KEY_CURRENCY = (SELECT $KEY_CODE FROM $TABLE_CURRENCIES WHERE $KEY_ROWID = ?))"
            } else ""

        fun accountSelectionV2(accountGrouping: AccountGrouping<*>) =
            if (accountGrouping == AccountGrouping.NONE) null
            else "$KEY_ACCOUNTID IN (SELECT $KEY_ROWID FROM $TABLE_ACCOUNTS WHERE " +
            when(accountGrouping) {
                AccountGrouping.CURRENCY -> KEY_CURRENCY
                AccountGrouping.FLAG -> KEY_FLAG
               AccountGrouping.TYPE -> KEY_TYPE
            } + " = ?)"

        fun accountSelectionArgs(accountId: Long): Array<String>? {
            return if (accountId == HOME_AGGREGATE_ID) null else arrayOf(
                abs(accountId).toString()
            )
        }

        fun accountSelectionArgsV2(accountGrouping: AccountGrouping<*>, group: String): Array<String>? {
            return if (accountGrouping == AccountGrouping.NONE) null else arrayOf(group)
        }
    }
}