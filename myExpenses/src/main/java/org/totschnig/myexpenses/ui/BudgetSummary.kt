package org.totschnig.myexpenses.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.budget_summary.view.*
import kotlinx.android.synthetic.main.budget_total_table.view.*
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BudgetActivity.getBackgroundForAvailable
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.util.ColorUtils.getComplementColor
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.viewmodel.data.Budget

class BudgetSummary @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        LayoutInflater.from(context).inflate(R.layout.budget_summary, this, true)
        val ta = context.obtainStyledAttributes(attrs, R.styleable.BudgetSummary)
        val withAllocated = ta.getBoolean(R.styleable.BudgetSummary_withAllocated, true)
        ta.recycle()
        if (!withAllocated) {
            allocatedLabel.isVisible = false
            totalAllocated.isVisible = false
        }
    }

    fun setOnBudgetClickListener(listener: OnClickListener) {
        totalBudget.setOnClickListener(listener)
    }

    fun setAllocated(allocated: String) {
        totalAllocated.setText(allocated)
    }

    fun bind(budget: Budget, spent: Long, currencyFormatter: CurrencyFormatter) {
        budgetProgressTotal.setFinishedStrokeColor(budget.color)
        budgetProgressTotal.setUnfinishedStrokeColor(getComplementColor(budget.color))
        totalBudget.text = currencyFormatter.formatCurrency(budget.amount)
        totalAmount.text = currencyFormatter.formatCurrency(Money(budget.currency, -spent))
        val allocated = budget.amount.getAmountMinor()
        val available = allocated - spent
        totalAvailable.text = currencyFormatter.formatCurrency(Money(budget.currency, available))
        val onBudget = available >= 0
        totalAvailable.setBackgroundResource(getBackgroundForAvailable(onBudget))
        totalAvailable.setTextColor(context.resources.getColor(if (onBudget) R.color.colorIncome else R.color.colorExpense))
        val progress = if (allocated == 0L) 100 else Math.round(spent * 100f / allocated)
        UiUtils.configureProgress(budgetProgressTotal, progress)
    }
}