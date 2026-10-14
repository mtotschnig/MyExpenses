---
domain: budgeting
availability:
- {place: android-app}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/BudgetActivity.kt#dispatchCommand
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/BudgetViewModel2.kt#deleteBudget
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/db2/RepositoryBudget.kt#deleteBudget
---

# Delete budget

The User deletes a budget with its allocations.
