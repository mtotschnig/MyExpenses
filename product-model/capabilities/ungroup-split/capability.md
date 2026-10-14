---
domain: transactions
availability:
- {place: android-app}
references:
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/activity/BaseMyExpenses.kt#ungroupSplit
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/viewmodel/MyExpensesViewModel.kt#revokeSplit
---

# Ungroup split

The User turns a split transaction back into ordinary transactions, one per part.
