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

import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.DatabaseConstants

@Parcelize
@Serializable
@SerialName(DatabaseConstants.KEY_PAYEEID)
class PayeeCriterion(
    override val label: String,
    override val values: List<Long>
) : IdCriterion() {
    constructor(label: String = "", vararg values: Long) : this(label, values.toList())

    @IgnoredOnParcel
    override val id: Int = R.id.FILTER_PAYEE_COMMAND
    @IgnoredOnParcel
    override val column = DatabaseConstants.KEY_PAYEEID
    @IgnoredOnParcel
    override val title = R.string.payer_or_payee

    override fun getSelection(forExport: Boolean): String {
        return if (operation === WhereFilter.Operation.ISNULL) {
            super.getSelection(false)
        } else {
            val selection = WhereFilter.Operation.IN.getOp(values.size)
            (column + " IN (SELECT " + DatabaseConstants.KEY_ROWID + " FROM "
                    + DatabaseConstants.TABLE_PAYEES + " WHERE " + DatabaseConstants.KEY_PARENTID + " " + selection + " OR "
                    + DatabaseConstants.KEY_ROWID + " " + selection + ")")
        }
    }

    override val selectionArgs: Array<String>
        get() = arrayOf(*super.selectionArgs, *super.selectionArgs)

    override val shouldApplyToSplitTransactions get() = false

    companion object {
        fun fromStringExtra(extra: String) =
            if (extra == "null") PayeeCriterion() else
                parseStringExtra(extra)?.let {
                    PayeeCriterion(it.first, *it.second)
                }
    }
}