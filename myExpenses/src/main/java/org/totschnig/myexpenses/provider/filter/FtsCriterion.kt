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
data class FtsCriterion(
    val searchString: String?,
    val includeAccountLabel: Boolean = true
) : SimpleCriterion<String>() {

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

    override fun prettyPrint(context: Context) = searchString ?: context.getString(R.string.empty)

    override fun getSelection(forExport: Boolean): String {
        if (searchString.isNullOrBlank()) return "1 = 1"
        return if (includeAccountLabel) {
            "$KEY_ROWID IN (SELECT transaction_id FROM $TABLE_TRANSACTIONS_FTS WHERE $TABLE_TRANSACTIONS_FTS MATCH ?)"
        } else {
            "$KEY_ROWID IN (SELECT transaction_id FROM $TABLE_TRANSACTIONS_FTS WHERE comment MATCH ? OR payee_name MATCH ? OR category_name MATCH ? OR tag_names MATCH ? OR reference_number MATCH ?)"
        }
    }

    override val values: List<String>
        get() {
            val sanitized = searchString?.takeIf { it.isNotBlank() }?.let { sanitizeFtsQuery(it) }
                ?: return emptyList()
            return if (includeAccountLabel) listOf(sanitized) else List(5) { sanitized }
        }

    companion object : DisplayInfo {

        fun sanitizeFtsQuery(query: String): String {
            val trimmed = query.trim()
            return when {
                trimmed.startsWith("\"") && trimmed.endsWith("\"") -> trimmed

                else -> trimmed.split("\\s+".toRegex())
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { token ->
                        if (token.contains(":") && !token.startsWith("\"")) {
                            "\"${token.replace("\"", "\"\"")}\""
                        } else {
                            token.replace("\"", "\"\"")
                        }
                    }
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
