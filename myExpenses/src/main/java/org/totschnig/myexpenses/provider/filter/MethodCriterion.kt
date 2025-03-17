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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payment
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.DatabaseConstants

@Parcelize
@Serializable
@SerialName(DatabaseConstants.KEY_METHODID)
data class MethodCriterion(
    override val label: String,
    override val values: List<Long>
) : IdCriterion() {
    constructor(label: String = "", vararg values: Long) : this(label, values.toList())

    @IgnoredOnParcel
    override val id = R.id.FILTER_METHOD_COMMAND
    @IgnoredOnParcel
    override val column = DatabaseConstants.KEY_METHODID

    override val displayInfo: DisplayInfo
        get() = MethodCriterion

    override val shouldApplyToSplitTransactions get() = false

    companion object: DisplayInfo {
        fun fromStringExtra(extra: String) =
            if (extra == "null") MethodCriterion() else
                parseStringExtra(extra)?.let {
                    MethodCriterion(it.first, *it.second)
                }

        override val title = R.string.method
        override val extendedTitle = R.string.search_method
        override val icon = Icons.Default.Payment
        override val clazz = MethodCriterion::class
    }
}