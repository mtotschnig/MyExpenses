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
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/BudgetViewModel2.kt#rollOverTotal
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/BudgetViewModel2.kt#rollOverCategories
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/BudgetViewModel2.kt#rollOverSave
---

# Roll over budget

The User carries what is left of a period into the next one, for the whole budget or per category.
