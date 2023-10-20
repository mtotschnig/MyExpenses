package org.totschnig.myexpenses.viewmodel.data

data class BudgetProgress(
    val title: String,
    val allocated: Long,
    val spent: Long,
    val totalDays: Long,
    val currentDay: Long
) {
    val remainingBudget: Long
        get() = allocated - spent
    val remainingDays = totalDays - currentDay
}