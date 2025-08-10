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

@Deprecated("This is only used by FilterPersistenceLegacy for migration to version 3.9.4")
data class WhereFilter(val criteria: List<SimpleCriterion<*>> = emptyList()) {

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

    companion object {
        fun empty() = WhereFilter()
    }
}