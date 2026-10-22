---
appliesTo:
- {type: capability, id: roll-over-budget}
- {type: capability, id: delete-budget-rollover}
- {type: entity, id: budget, facts: [Rollovers]}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/BudgetActivity.kt#onPrepareOptionsMenu
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/BudgetViewModel2.kt#rollOverTotal
---

# Rollovers exist only for repeating budgets

A remainder is rolled over only from one period of a repeating budget into the next, once per period, and a rollover never exceeds what is left.
