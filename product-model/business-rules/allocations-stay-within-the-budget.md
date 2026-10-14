---
appliesTo:
- {type: capability, id: change-budget-allocation}
- {type: entity, id: budget, facts: [Category allocations, Budgeted amount]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/BudgetActivity.kt#showEditBudgetDialog
---

# Allocations stay within the budget

The allocations to main categories never exceed the budgeted amount, and those to sub categories never exceed their main category's allocation; an allocation is never below what its sub categories have, less the rollover it received.
