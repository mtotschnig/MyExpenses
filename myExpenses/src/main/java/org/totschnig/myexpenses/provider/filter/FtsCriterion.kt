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

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.KEY_FTS
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.TABLE_TRANSACTIONS_FTS

@Parcelize
@Serializable
@SerialName(KEY_FTS)
data class FtsCriterion(val searchString: String?) : SimpleCriterion<String>() {

    @IgnoredOnParcel
    override val id = R.id.FILTER_FTS_COMMAND

    override val displayInfo: DisplayInfo
        get() = FtsCriterion

    @IgnoredOnParcel
    override val column = KEY_ROWID

    @IgnoredOnParcel
    override val operation = Operation.LIKE

    override val isNullable: Boolean
        get() = true

    override val isNull: Boolean
        get() = searchString == null

    override fun prettyPrint(context: Context) =
        searchString ?: context.getString(R.string.empty)

    override fun getSelection(forExport: Boolean): String {
        return if (searchString.isNullOrBlank()) {
            "1 = 1"
        } else {
            "$KEY_ROWID IN (SELECT transaction_id FROM $TABLE_TRANSACTIONS_FTS WHERE $TABLE_TRANSACTIONS_FTS MATCH ?)"
        }
    }

    override val values: List<String>
        get() = listOfNotNull(searchString?.takeIf { it.isNotBlank() }?.let { sanitizeFtsQuery(it) })

    companion object : DisplayInfo {

        fun sanitizeFtsQuery(query: String): String {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return ""
            // If already quoted or using explicit FTS syntax, leave as is
            if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                return trimmed
            }
            // Escape double quotes inside terms
            val escaped = trimmed.replace("\"", "\"\"")
            // Wrap terms or tokenize simple keywords for wildcard prefix search if single word
            return if (!escaped.contains(" ") && !escaped.contains("*") && !escaped.contains("\"") && !escaped.startsWith("-")) {
                "$escaped*"
            } else {
                escaped
            }
        }

        fun fromStringExtra(extra: String) = FtsCriterion(extra)
        override val title = R.string.full_text_search
        override val extendedTitle = R.string.search_full_text
        override val icon = Icons.Default.Search
        override val isPartial = true
        override val clazz = FtsCriterion::class
    }
}
