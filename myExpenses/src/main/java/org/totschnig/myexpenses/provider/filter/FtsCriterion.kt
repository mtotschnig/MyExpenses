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

    override fun prettyPrint(context: Context) = searchString ?: context.getString(R.string.empty)

    private fun parseTerms(): Pair<String, List<String>> {
        val trimmed = searchString?.trim().orEmpty()
        if (trimmed.isEmpty()) return "" to emptyList()

        val regex = "-?(?:[A-Za-z_]+:)?\"[^\"]*\"|\\S+".toRegex()
        val matches = regex.findAll(trimmed).map { it.value }.toList()

        val positiveTokens = mutableListOf<String>()
        val negativeTokens = mutableListOf<String>()

        for (rawToken in matches) {
            val isNegated = rawToken.startsWith("-") && rawToken.length > 1
            val token = if (isNegated) rawToken.substring(1) else rawToken

            if (isNegated) {
                negativeTokens.add(token)
            } else {
                positiveTokens.add(token)
            }
        }

        val positiveQuery = positiveTokens.joinToString(" ")
        return positiveQuery to negativeTokens
    }

    @Suppress("MaxLineLength", "ktlint:standard:max-line-length")
    override fun getSelection(forExport: Boolean): String {
        if (searchString.isNullOrBlank()) return "1 = 1"
        val (positiveQuery, negativeTokens) = parseTerms()

        val clauses = mutableListOf<String>()
        if (positiveQuery.isNotBlank()) {
            clauses.add(
                "$KEY_ROWID IN (SELECT rowid FROM $TABLE_TRANSACTIONS_FTS WHERE $TABLE_TRANSACTIONS_FTS MATCH ?)"
            )
        }
        for (neg in negativeTokens) {
            if (neg.isNotBlank()) {
                clauses.add(
                    "$KEY_ROWID NOT IN (SELECT rowid FROM $TABLE_TRANSACTIONS_FTS WHERE $TABLE_TRANSACTIONS_FTS MATCH ?)"
                )
            }
        }

        return if (clauses.isEmpty()) "1 = 1" else clauses.joinToString(" AND ")
    }

    override val values: List<String>
        get() {
            if (searchString.isNullOrBlank()) return emptyList()
            val (positiveQuery, negativeTokens) = parseTerms()

            val result = mutableListOf<String>()
            if (positiveQuery.isNotBlank()) {
                result.add(sanitizeFtsQuery(positiveQuery))
            }
            for (neg in negativeTokens) {
                if (neg.isNotBlank()) {
                    result.add(sanitizeFtsQuery(neg))
                }
            }
            return result
        }

    companion object : DisplayInfo {

        fun sanitizeFtsQuery(query: String): String {
            var trimmed = query.trim()
            if (trimmed.isEmpty()) return ""

            // Auto-balance unmatched quotes if user typed a single quote
            if (trimmed.count { it == '"' } % 2 != 0) {
                trimmed += "\""
            }

            return trimmed
        }

        fun fromStringExtra(extra: String) = FtsCriterion(extra)
        override val title = R.string.full_text_search
        override val extendedTitle = R.string.search_full_text
        override val icon = Icons.Default.Search
        override val isPartial = true
        override val clazz = FtsCriterion::class
    }
}
