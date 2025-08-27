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
import androidx.compose.material.icons.automirrored.filled.Notes
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.DatabaseConstants
import kotlin.reflect.KClass

@Parcelize
@Serializable
@SerialName(DatabaseConstants.KEY_COMMENT)
data class CommentCriterion(override val searchString: String?) : TextCriterion() {

    @IgnoredOnParcel
    override val id = R.id.FILTER_COMMENT_COMMAND

    override val displayInfo: DisplayInfo
        get() = CommentCriterion

    @IgnoredOnParcel
    override val column = DatabaseConstants.KEY_COMMENT

    companion object: DisplayInfo {

        fun fromStringExtra(extra: String) = CommentCriterion(extra)
        override val title = R.string.comment
        override val extendedTitle = R.string.search_comment
        override val icon = Icons.AutoMirrored.Default.Notes
        override val isPartial = true
        override val clazz = CommentCriterion::class
    }
}