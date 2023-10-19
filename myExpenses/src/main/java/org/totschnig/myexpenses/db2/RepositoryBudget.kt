package org.totschnig.myexpenses.db2

import org.totschnig.myexpenses.viewmodel.data.BudgetProgress

fun Repository.loadBudgetProgress(budgetId: Long) = BudgetProgress(
    "Debug Budget", 6000, 4000, 30, 23
)