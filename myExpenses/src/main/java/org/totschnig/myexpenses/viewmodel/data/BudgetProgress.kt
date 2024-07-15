package org.totschnig.myexpenses.viewmodel.data

import org.totschnig.myexpenses.db2.BudgetPeriod
import org.totschnig.myexpenses.model.CurrencyUnit

data class BudgetProgress(
    val title: String,
    val currency: String,
    val groupInfo: BudgetPeriod,
    val allocated: Long,
    val spent: Long,
    val totalDays: Long,
    /**
     *  currentDay is today related to start of period, i.e:
     *  it is < 0 for future periods and > totalDays for past periods
     */
    val currentDay: Long
) {
    val remainingBudget: Long = allocated - spent
    val remainingDays = totalDays - currentDay.coerceAtLeast(0)
}