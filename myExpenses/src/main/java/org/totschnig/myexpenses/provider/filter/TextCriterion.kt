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
import kotlinx.serialization.Serializable
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.Utils

@Serializable
sealed class TextCriterion : SimpleCriterion<String>() {

    abstract val searchString : String?

    override fun getSelection(forExport: Boolean): String {
        return if (searchString == null) "IFNULL(TRIM($column), '') = ''" else super.getSelection(forExport)
    }

    override val values: List<String>
        get() = listOfNotNull(
            searchString?.let { "%${Utils.escapeSqlLikeExpression(it)}%" }
        )

    override val operation = Operation.LIKE

    override val isNullable: Boolean
        get() = true

    override val isNull: Boolean
        get() = searchString == null

    override fun prettyPrint(context: Context) =
        searchString ?: context.getString(R.string.empty)
}