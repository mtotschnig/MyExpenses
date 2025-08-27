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
package org.totschnig.myexpenses.provider.filter

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.DatabaseConstants
import kotlin.reflect.KClass

const val ACCOUNT_COLUMN = DatabaseConstants.KEY_ACCOUNTID

@Parcelize
@Serializable
@SerialName(DatabaseConstants.KEY_ACCOUNTID)
data class AccountCriterion(
    override val label: String,
    override val values: List<Long>
) : IdCriterion() {
    constructor(label: String, vararg values: Long) : this(label, values.asList())

    @IgnoredOnParcel
    override val id = R.id.FILTER_ACCOUNT_COMMAND

    @IgnoredOnParcel
    override val column = ACCOUNT_COLUMN

    override val displayInfo: DisplayInfo
        get() = AccountCriterion

    override fun getSelection(forExport: Boolean): String {
        val selection = operation.getOp(values.size)
        return "$column $selection"
    }

    companion object : DisplayInfo {
        fun fromStringExtra(extra: String) = parseStringExtra(extra)?.let {
            AccountCriterion(it.first, *it.second)
        }

        override val title = R.string.account
        override val extendedTitle = R.string.search_account
        override val icon = Icons.Default.AccountBalance
        override val clazz = AccountCriterion::class
    }
}