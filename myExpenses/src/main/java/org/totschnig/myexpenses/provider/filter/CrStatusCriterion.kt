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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Square
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.provider.DatabaseConstants

@Parcelize
@Serializable
@SerialName(DatabaseConstants.KEY_CR_STATUS)
data class CrStatusCriterion(override val values: List<CrStatus>) : SimpleCriterion<CrStatus>() {

    @IgnoredOnParcel
    override val id: Int = R.id.FILTER_STATUS_COMMAND
    @IgnoredOnParcel
    override val column = DatabaseConstants.KEY_CR_STATUS
    @IgnoredOnParcel
    override val operation = Operation.IN

    override val displayInfo: DisplayInfo
        get() = CrStatusCriterion

    override fun prettyPrint(context: Context) =
        values.joinToString(",") { context.getString(it.toStringRes()) }

    override val shouldApplyToSplitTransactions get() = false

    companion object: DisplayInfo {

        fun fromStringExtra(extra: String) =
            CrStatusCriterion(extra.split(EXTRA_SEPARATOR).mapNotNull {
                try {
                    CrStatus.valueOf(it)
                } catch (_: IllegalArgumentException) {
                    null
                }
            }.toList())

        override val title = R.string.status
        override val extendedTitle = R.string.search_status
        override val icon = Icons.Default.Square
        override val clazz = CrStatusCriterion::class
    }
}