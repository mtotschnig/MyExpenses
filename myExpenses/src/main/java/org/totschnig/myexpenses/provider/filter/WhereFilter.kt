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

import androidx.annotation.CheckResult
import androidx.compose.runtime.Immutable
import org.totschnig.myexpenses.provider.CTE_SEARCH

@Immutable
data class WhereFilter(val criteria: List<SimpleCriterion<*>> = emptyList()) {

    /**
     * @param tableName If not null, we are guaranteed to be called in the context of single account,
     * i.e. not for an aggregate account, and we reference the passed in name, otherwise we reference CTE_SEARCH
     */
    fun getSelectionForParents(tableName: String? = null): String {
        return criteria.joinToString(" AND ") {
            it.getSelectionForParents(
                tableName ?: CTE_SEARCH,
                tableName != null
            )
        }
    }

    fun getSelectionForParts(tableName: String? = null) =
        criteria.joinToString(" AND ") { it.getSelectionForParts(tableName ?: CTE_SEARCH) }

    fun getSelectionArgsList(queryParts: Boolean) = criteria.flatMap {
        (when {
            queryParts || (it.shouldApplyToArchive xor it.shouldApplyToSplitTransactions ) -> it.selectionArgs + it.selectionArgs
            it.shouldApplyToSplitTransactions && it.shouldApplyToArchive -> it.selectionArgs + it.selectionArgs + it.selectionArgs
            else -> it.selectionArgs
        })
            .asList()
    }

    fun getSelectionArgs(queryParts: Boolean) = getSelectionArgsList(queryParts).toTypedArray()

    fun getSelectionArgsIfNotEmpty(queryParts: Boolean) = getSelectionArgs(queryParts)
        .takeIf { it.isNotEmpty() }

    operator fun get(id: Int): SimpleCriterion<*>? = criteria.find { it.id == id }

    operator fun get(column: String) = criteria.find { it.column == column }

    /**
     * returns a new filter with criterion added
     */
    @CheckResult
    fun put(criterion: SimpleCriterion<*>): WhereFilter {
        val existing = indexOf(criterion.id)
        return if (existing > -1) {
            copy(criteria = criteria.toMutableList().also {
                it[existing] = criterion
            })
        } else {
            copy(criteria = criteria + criterion)
        }
    }

    private fun indexOf(id: Int) = criteria.indexOfFirst { it.id == id }

    /**
     * returns a new filter with criterion with id removed
     */
    @CheckResult
    fun remove(id: Int): WhereFilter {
        val existing = indexOf(id)
        return if (existing > -1) {
            copy(criteria = criteria.toMutableList().also {
                it.removeAt(existing)
            })
        } else this
    }

    val isEmpty: Boolean
        get() = criteria.isEmpty()

    enum class Operation(private val op: String?) {
        NOPE(""), EQ("=?"), NEQ("!=?"), GT(">?"), GTE(">=?"), LT("<?"), LTE("<=?"), BTW(
            "BETWEEN ? AND ?"
        ),
        ISNULL("is NULL"), LIKE("LIKE ? ESCAPE '$LIKE_ESCAPE_CHAR'"), IN(null);

        fun getOp(length: Int): String {
            if (this == IN) {
                val sb = StringBuilder()
                sb.append("IN (")
                for (i in 0 until length) {
                    sb.append("?")
                    if (i < length - 1) {
                        sb.append(",")
                    }
                }
                sb.append(")")
                return sb.toString()
            }
            return op!!
        }
    }

    companion object {
        const val LIKE_ESCAPE_CHAR = "\\"
        fun empty(): WhereFilter {
            return WhereFilter()
        }
    }
}