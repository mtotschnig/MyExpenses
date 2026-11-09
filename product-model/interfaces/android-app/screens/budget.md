---
entities:
- {entity: budget, shows: [Title, Period, Filters, Budgeted amount, Category allocations, Rollovers, Spent, Available], collects: [Budgeted amount, Category allocations, Rollovers]}
- {entity: category, shows: [Name]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/BudgetActivity.kt#BudgetActivity
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/BudgetViewModel2.kt
---

# Budget

One budget for one period: allocations, rollovers, spending and what is available per category.
