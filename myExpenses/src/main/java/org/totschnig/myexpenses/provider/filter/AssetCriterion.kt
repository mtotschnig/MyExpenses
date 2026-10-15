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
import androidx.compose.material.icons.automirrored.filled.ShowChart
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.KEY_TRANSFER_ACCOUNT

@Parcelize
@Serializable
@SerialName("asset")
data class AssetCriterion(
    override val label: String,
    override val values: List<Long>
) : IdCriterion() {
    constructor(label: String, vararg values: Long) : this(label, values.toList())

    @IgnoredOnParcel
    override val id = R.id.FILTER_ACCOUNT_COMMAND

    @IgnoredOnParcel
    override val column = KEY_TRANSFER_ACCOUNT

    override val displayInfo: DisplayInfo
        get() = AssetCriterion

    override fun getSelection(forExport: Boolean): String {
        val selection = operation.getOp(values.size)
        return "($column $selection OR $KEY_ACCOUNTID $selection)"
    }

    override val selectionArgs: Array<String>
        get() = arrayOf(*super.selectionArgs, *super.selectionArgs)

    companion object : DisplayInfo {
        fun fromStringExtra(extra: String) = parseStringExtra(extra)?.let {
            AssetCriterion(it.first, *it.second)
        }

        override val title = R.string.trade_target_asset
        override val extendedTitle = R.string.search_asset
        override val icon = Icons.AutoMirrored.Filled.ShowChart
        override val clazz = AssetCriterion::class
    }
}
