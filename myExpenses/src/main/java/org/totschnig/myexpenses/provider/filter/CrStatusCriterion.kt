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
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.provider.DatabaseConstants

@Parcelize
class CrStatusCriterion(override val values: Array<CrStatus>) : Criterion<CrStatus>() {

    @IgnoredOnParcel
    override val id: Int = R.id.FILTER_STATUS_COMMAND
    @IgnoredOnParcel
    override val column = DatabaseConstants.KEY_CR_STATUS
    @IgnoredOnParcel
    override val operation = WhereFilter.Operation.IN

    override fun prettyPrint(context: Context) =
        values.joinToString(",") { context.getString(it.toStringRes()) }

    override fun toString() = values.joinToString(EXTRA_SEPARATOR)

    override fun shouldApplyToParts(): Boolean {
        return false
    }

    companion object {

        fun fromStringExtra(extra: String) =
            CrStatusCriterion(extra.split(EXTRA_SEPARATOR).mapNotNull {
                try {
                    CrStatus.valueOf(it)
                } catch (e: java.lang.IllegalArgumentException) {
                    null
                }
            }.toTypedArray())
    }
}