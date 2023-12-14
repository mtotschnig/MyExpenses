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
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.DatabaseConstants

@Parcelize
class TransferCriterion(
    override val label: String?,
    override val values: Array<Long>
) : IdCriterion() {
    constructor(label: String, vararg values: Long) : this(label, values.toTypedArray())

    @IgnoredOnParcel
    override val operation = WhereFilter.Operation.IN

    override fun getSelection(forExport: Boolean): String {
        val selection = operation.getOp(values.size)
        return "${DatabaseConstants.KEY_TRANSFER_PEER} IS NOT NULL AND ($column $selection OR ${DatabaseConstants.KEY_ACCOUNTID} $selection)"
    }

    override val selectionArgs: Array<String>
        get() = arrayOf(*super.selectionArgs, *super.selectionArgs)

    @IgnoredOnParcel
    override val id = R.id.FILTER_TRANSFER_COMMAND

    @IgnoredOnParcel
    override val column = DatabaseConstants.KEY_TRANSFER_ACCOUNT

    companion object {

        fun fromStringExtra(extra: String) = parseStringExtra(extra)?.let {
            TransferCriterion(it.first, *it.second)
        }
    }
}