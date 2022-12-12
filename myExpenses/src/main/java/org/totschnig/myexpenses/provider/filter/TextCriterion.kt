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
import org.totschnig.myexpenses.util.Utils

abstract class TextCriterion : Criterion<String>() {

    abstract val searchString : String

    override val values: Array<String>
        get() = arrayOf("%${Utils.escapeSqlLikeExpression(searchString)}%")

    override val operation = WhereFilter.Operation.LIKE

    override fun prettyPrint(context: Context): String {
        return searchString
    }

    override fun toString(): String {
        return searchString
    }
}