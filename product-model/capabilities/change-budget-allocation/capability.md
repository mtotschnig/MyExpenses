---
domain: budgeting
availability:
- {place: android-app}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/BudgetActivity.kt#showEditBudgetDialog
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/BudgetViewModel2.kt#updateBudget
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/provider/BaseTransactionProvider.kt#budgetCategoryUpsert
---

# Change budget allocation

The User sets the budgeted amount or a category's allocation, for every period or for the shown period only.
