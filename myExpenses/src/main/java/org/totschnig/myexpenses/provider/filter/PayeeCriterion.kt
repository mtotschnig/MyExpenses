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
class PayeeCriterion(
    override val label: String?,
    override val operation: WhereFilter.Operation,
    override val values: Array<Long>
) : IdCriterion() {
    constructor() : this(null, WhereFilter.Operation.ISNULL, emptyArray())
    constructor(label: String, vararg values: Long) : this(label, WhereFilter.Operation.IN, values.toTypedArray())

    @IgnoredOnParcel
    override val id: Int = R.id.FILTER_PAYEE_COMMAND

    @IgnoredOnParcel
    override val column = DatabaseConstants.KEY_PAYEEID


    override fun shouldApplyToParts() = false

    companion object {
        fun fromStringExtra(extra: String) =
            if (extra == "null") PayeeCriterion() else
                parseStringExtra(extra)?.let {
                    PayeeCriterion(it.first, *it.second)
                }
    }
}