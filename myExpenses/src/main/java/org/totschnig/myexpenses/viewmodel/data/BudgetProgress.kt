package org.totschnig.myexpenses.viewmodel.data

data class BudgetProgress(
    val title: String,
    val allocated: Long,
    val spent: Long,
    val totalDays: Int,
    val currentDay: Int
)