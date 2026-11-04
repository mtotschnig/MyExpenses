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
package org.totschnig.myexpenses.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.util.GroupingInfo
import org.totschnig.myexpenses.viewmodel.data.Category

sealed interface ContribPayload : Parcelable {

    @Parcelize
    data class Budget(val budgetId: Long, val headerId: Int) : ContribPayload

    @Parcelize
    data class Banking(val bankId: Long, val accountId: Long, val accountTypeId: Long) : ContribPayload

    @Parcelize
    data class GroupToSplit(val rowIds: LongArray? = null) : ContribPayload {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is GroupToSplit) return false
            return rowIds?.contentEquals(other.rowIds ?: longArrayOf()) ?: (other.rowIds == null)
        }

        override fun hashCode(): Int = rowIds?.contentHashCode() ?: 0
    }

    @Parcelize
    data class InstantiateSplitTemplateSave(val rowIds: LongArray? = null) : ContribPayload {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is InstantiateSplitTemplateSave) return false
            return rowIds?.contentEquals(other.rowIds ?: longArrayOf()) ?: (other.rowIds == null)
        }

        override fun hashCode(): Int = rowIds?.contentHashCode() ?: 0
    }

    @Parcelize
    data class CategoryTree(
        val command: Int,
        val category: Category? = null,
        val saveParams: SaveCategoryParams? = null
    ) : ContribPayload

    @Parcelize
    data class SaveCategoryParams(
        val label: String,
        val icon: String?,
        val typeFlags: Byte,
        val parentId: Long?
    ) : Parcelable

    @Parcelize
    data class Synchronization(val itemId: Int) : ContribPayload

    @Parcelize
    data class Print(val printType: Int) : ContribPayload

    @Parcelize
    data class Distribution(val groupingInfo: GroupingInfo) : ContribPayload

    @Parcelize
    data class InstantiateSplitTemplateEdit(val templateId: Long) : ContribPayload
}
