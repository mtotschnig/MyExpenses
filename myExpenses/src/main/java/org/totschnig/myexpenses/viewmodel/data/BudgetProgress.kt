package org.totschnig.myexpenses.viewmodel.data

import org.totschnig.myexpenses.model.CurrencyUnit

data class BudgetProgress(
    val title: String,
    val currency: CurrencyUnit,
    val groupInfo: String,
    val allocated: Long,
    val spent: Long,
    val totalDays: Long,
    val currentDay: Long
) {
    val remainingBudget: Long
        get() = allocated - spent
    val remainingDays = totalDays - currentDay
}