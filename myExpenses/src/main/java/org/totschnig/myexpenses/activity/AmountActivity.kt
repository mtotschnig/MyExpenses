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
package org.totschnig.myexpenses.activity

import android.view.ViewGroup
import android.widget.TextView
import org.totschnig.myexpenses.ui.AmountInput
import org.totschnig.myexpenses.ui.ExchangeRateEdit
import java.math.BigDecimal

abstract class AmountActivity : EditActivity() {
    abstract val amountLabel: TextView
    abstract val amountRow: ViewGroup
    abstract val exchangeRateRow: ViewGroup
    abstract val amountInput: AmountInput
    abstract val exchangeRateEdit: ExchangeRateEdit

    /**
     * @return true for income, false for expense
     */
    protected val isIncome: Boolean
        get() = amountInput.type

    protected open fun onTypeChanged(isChecked: Boolean) {
        setDirty()
        configureType()
    }

    protected open fun configureType() {}

    protected fun validateAmountInput(showToUser: Boolean): BigDecimal? {
        return validateAmountInput(amountInput, showToUser)
    }

    override fun setupListeners() {
        amountInput.addTextChangedListener(this)
        amountInput.setTypeChangedListener { isChecked: Boolean -> onTypeChanged(isChecked) }
    }
}